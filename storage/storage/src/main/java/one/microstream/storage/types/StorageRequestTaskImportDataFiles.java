package one.microstream.storage.types;

/*-
 * #%L
 * microstream-storage
 * %%
 * Copyright (C) 2019 - 2021 MicroStream Software
 * %%
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is
 * available at https://www.gnu.org/software/classpath/license.html.
 * 
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 * #L%
 */

import java.util.concurrent.atomic.AtomicBoolean;

import one.microstream.X;
import one.microstream.afs.types.AFS;
import one.microstream.afs.types.AFile;
import one.microstream.collections.XArrays;
import one.microstream.collections.types.XGettingEnum;
import one.microstream.concurrency.XThreads;
import one.microstream.persistence.binary.types.Binary;
import one.microstream.storage.exceptions.StorageException;
import one.microstream.storage.exceptions.StorageExceptionImportFailed;
import one.microstream.storage.types.StorageDataFileItemIterator.ItemProcessor;


public interface StorageRequestTaskImportDataFiles extends StorageRequestTask
{
	public final class Default
	extends StorageChannelSynchronizingTask.AbstractCompletingTask<Void>
	implements StorageRequestTaskImportDataFiles, StorageChannelTaskStoreEntities
	{
		///////////////////////////////////////////////////////////////////////////
		// constants //
		//////////////
		
		/* (14.11.2019 TM)TODO: weird waiting time
		 * This should be removed or at least configurable.
		 */
		private static final int SOURCE_FILE_WAIT_TIME_MS = 100;
		
		
		
		///////////////////////////////////////////////////////////////////////////
		// instance fields //
		////////////////////

		private final XGettingEnum<AFile>           importFiles           ;
		private final StorageEntityCache.Default[]  entityCaches          ;
		private final StorageObjectIdRangeEvaluator objectIdRangeEvaluator;
		
		// adding point for the reader
		private final StorageImportSourceFile.Default[] sourceFileHeads;
		
		// starting point for the channels to process
		private final StorageImportSourceFile.Default[] sourceFileTails;

		private final AtomicBoolean    complete  = new AtomicBoolean();
		private volatile long    maxObjectId; //TODO Check, why it is not assigned?
		private          Thread  readThread ;



		///////////////////////////////////////////////////////////////////////////
		// constructors //
		/////////////////

		Default(
			final long                          timestamp             ,
			final int                           channelCount          ,
			final StorageObjectIdRangeEvaluator objectIdRangeEvaluator,
			final XGettingEnum<AFile>           importFiles,
			final StorageOperationController    controller
		)
		{
			// every channel has to store at least a chunk header, so progress count is always equal to channel count
			super(timestamp, channelCount, controller);
			this.importFiles            = importFiles;
			this.objectIdRangeEvaluator = objectIdRangeEvaluator;
			this.entityCaches           = new StorageEntityCache.Default[channelCount];
			this.sourceFileTails        = createSourceFileSlices(channelCount);
			this.sourceFileHeads        = this.sourceFileTails.clone();
		}

		
		
		///////////////////////////////////////////////////////////////////////////
		// methods //
		////////////
		
		private static StorageImportSourceFile.Default[] createSourceFileSlices(final int channelCount)
		{
			final StorageImportSourceFile.Default[] sourceFileTails = new StorageImportSourceFile.Default[channelCount];
			for(int i = 0; i < channelCount; i++)
			{
				sourceFileTails[i] = new StorageImportSourceFile.Default(i, null, null);
			}
			
			return sourceFileTails;
		}

		private boolean entityCacheCollectionNotComplete()
		{
			for(final StorageEntityCache.Default entityCache : this.entityCaches)
			{
				if(entityCache == null)
				{
					return true;
				}
			}
			return false;
		}

		private synchronized void ensureReaderThread()
		{
			if(this.readThread != null || this.entityCacheCollectionNotComplete())
			{
				return;
			}
			this.readThread = XThreads.start(this::readFiles);
		}

		final void readFiles()
		{
			final ItemReader itemReader = new ItemReader(this.entityCaches, this.sourceFileHeads);
			
			final StorageDataFileItemIterator iterator = StorageDataFileItemIterator.New(
				StorageDataFileItemIterator.BufferProvider.New(),
				itemReader
			);

			for(final AFile file : this.importFiles)
			{
//				DEBUGStorage.println("Reader reading source file " + file);
				try
				{
					itemReader.setSourceFile(file);
					AFS.execute(file, rf -> iterator.iterateStoredItems(rf));
					itemReader.completeCurrentSourceFile();
				}
				catch(final Exception e)
				{
					throw new StorageExceptionImportFailed("Exception while reading import file " + file, e);
				}
			}
//			DEBUGStorage.println("* completed reading source files");
			this.complete.set(true);
		}

		@Override
		protected final Void internalProcessBy(final StorageChannel channel)
		{
			/*
			 * signal the channel to prepare for the import
			 * (validate type dictionary, keep current head file and create a new one)
			 */
			synchronized(this.entityCaches)
			{
				this.entityCaches[channel.channelIndex()] = channel.prepareImportData();
			}

			/*
			 * the last thread to enter this method starts a single reader thread,
			 * all other threads return here right away
			 */
			this.ensureReaderThread();

			// the tail array is always initialized with an empty dummy source file which serves as an entry point.
			StorageImportSourceFile.Default currentSourceFile = this.sourceFileTails[channel.channelIndex()];

			// quite a braces mountain, however it is logically necessary
			try
			{
				importLoop:
				while(true)
				{
					// acquire a lock on the channel-exclusive signalling instance to wait for the reader's notification
					synchronized(currentSourceFile)
					{
						// wait for the next batch to import (successor of the current batch)
						while(currentSourceFile.next == null)
						{
							if(this.complete.get())
							{
//								DEBUGStorage.println(channel.channelIndex() + " done importing.");
								// there will be no more next source file, so abort (task is complete)
								break importLoop;
							}
//							DEBUGStorage.println(channel.channelIndex() + " waiting for next on " + currentSourceFile);
							// better check again after some time, indefinite wait caused a deadlock once
							// (16.04.2016)TODO: isn't the above comment a bug? Test and change or comment better.
							currentSourceFile.wait(SOURCE_FILE_WAIT_TIME_MS);
							// note: completion adds an empty dummy source file to avoid special case handling here
						}
						// at this point, there definitely is a new/next batch to process, so advance tail and process
						currentSourceFile = currentSourceFile.next;
					}

//					DEBUGStorage.println(channel.channelIndex() + " importData() " + currentSourceFile);
					// process the batch outside the lock to not block the central reader thread by channel-local work
					channel.importData(currentSourceFile);
				}
			}
			catch(final InterruptedException e)
			{
				// being interrupted is a normal problem here, causing to abort the task, no further handling required.

				/* (16.04.2016)TODO: storage import interruption handling.
				 * Shouldn't an import be properly interruptible in the first place?
				 * Either change code or comment accordingly.
				 */
				throw new StorageException(e);
			}

			return null;
		}

		@Override
		protected final void succeed(final StorageChannel channel, final Void result)
		{
			// evaluate (validate or update if possible) objectId before committing the import
			this.objectIdRangeEvaluator.evaluateObjectIdRange(0, this.maxObjectId);

			/* on success, signal the channel to commit the imported data (register entities in cache)
			 * All channels use the same timestamp (this task's issuing timestamp) for consistency checks
			 */
			channel.commitImportData(this.timestamp());
		}

		@Override
		protected void postCompletionSuccess(final StorageChannel channel, final Void result)
			throws InterruptedException
		{
			this.cleanUpResources();
		}

		@Override
		protected final void fail(final StorageChannel channel, final Void result)
		{
			// on failure/abort, signal channel to rollback (delete newly created files and revert to last head file)
			this.cleanUpResources();
			channel.rollbackImportData(this.problemForChannel(channel));
		}

		private void cleanUpResources()
		{
			final DisruptionCollectorExecuting<StorageClosableFile> closer = DisruptionCollectorExecuting.New(fc ->
				fc.close()
			);
			
			for(final StorageImportSourceFile.Default s : this.sourceFileTails)
			{
				// the first slice is a dummy with no FileChannel instance
				for(StorageImportSourceFile.Default file = s; (file = file.next) != null;)
				{
//					DEBUGStorage.println("Closing: " + file);
					closer.executeOn(file);
				}
			}
			
			if(closer.hasDisruptions())
			{
				throw new StorageException(closer.toMultiCauseException());
			}
		}
		
		
		
		///////////////////////////////////////////////////////////////////////////
		// helper classes //
		///////////////////
		
		static final class ItemReader implements ItemProcessor
		{
			///////////////////////////////////////////////////////////////////////////
			// instance fields //
			////////////////////
			
			private final StorageEntityCache.Default[]      entityCaches             ;
			private final StorageImportSourceFile.Default[] sourceFileHeads          ;
			private final ChannelItem[]                     channelItems             ;
			private final int                               channelHash              ;
			private       AFile                             file                     ;
			private       int                               currentBatchChannel      ;
			private       long                              currentSourceFilePosition;
			private       long                              maxObjectId              ;

			
			
			///////////////////////////////////////////////////////////////////////////
			// constructors //
			/////////////////
			
			public ItemReader(
				final StorageEntityCache.Default[]      entityCaches   ,
				final StorageImportSourceFile.Default[] sourceFileHeads
			)
			{
				super();
				this.entityCaches    = entityCaches              ;
				this.sourceFileHeads = sourceFileHeads           ;
				this.channelHash     = sourceFileHeads.length - 1;
				this.channelItems    = XArrays.fill(
					new ChannelItem[sourceFileHeads.length],
					() ->
						new ChannelItem().resetChains()
				);
			}
			
			@Override
			public boolean accept(final long address, final long availableItemLength)
			{
				final long length = Binary.getEntityLengthRawValue(address);

				// check for a gap
				if(length < 0)
				{
//					DEBUGStorage.println("Gap    @" + this.currentSourceFilePosition + " [" + -length + "]");

					// keep track of current source file position to offset the next batch correctly
					this.currentSourceFilePosition += X.checkArrayRange(-length);

					// batch is effectively interrupted by the gap, even if the next entity belongs to the same channel
					this.currentBatchChannel = -1;

					// signal to calling context that item has been processed completely
					return true;
				}

				// check for incomplete entity header
				if(availableItemLength < Binary.entityHeaderLength())
				{
					// signal to calling context that entity cannot be processed and header must be reloaded
					return false;
				}

				final int intLength = X.checkArrayRange(length);

				// read and validate entity head information
				final long                      objectId     = Binary.getEntityObjectIdRawValue(address);
				final int                       channelIndex = (int)objectId & this.channelHash;
				final StorageEntityType.Default type         = this.entityCaches[channelIndex].validateEntity(
					intLength,
					Binary.getEntityTypeIdRawValue(address),
					objectId
				);

				// register entity accordingly (either new batch required or current batch can be enlarged)
				if(channelIndex != this.currentBatchChannel)
				{
					this.currentBatchChannel = channelIndex;
					this.startNewBatch(intLength, objectId, type);
				}
				else
				{
//					DEBUGStorage.println(
//						"Reader ADD batch Entity @" + this.currentSourceFilePosition
//						+ " [" + length + "] (" + this.currentBatchChannel + ") " + objectId
//					);
					this.addToCurrentBatch(intLength, objectId, type);
				}

				if(objectId >= this.maxObjectId)
				{
					this.maxObjectId = objectId;
				}

				// keep track of current source file position to offset the batch correctly
				this.currentSourceFilePosition += intLength;

				return true;
			}

			private void startNewBatch(
				final int                       length  ,
				final long                      objectId,
				final StorageEntityType.Default type
			)
			{
				final ChannelItem item = this.channelItems[this.currentBatchChannel];

//				DEBUGStorage.println(
//					"Reader NEW batch Entity @" + this.currentSourceFilePosition
//					+ " [" + length + "] (" + this.currentBatchChannel + ") " + objectId
//				);
				item.tailEntity = item.tailBatch = item.tailBatch.batchNext = new StorageChannelImportBatch.Default(
					this.currentSourceFilePosition,
					length,
					objectId,
					type
				);
			}

			private void addToCurrentBatch(
				final int                              length  ,
				final long                             objectId,
				final StorageEntityType.Default type
			)
			{
				final ChannelItem item = this.channelItems[this.currentBatchChannel];

				// intentionally ignores max file length for sake of import efficiency
				item.tailEntity = item.tailEntity.next = new StorageChannelImportEntity.Default(
					length,
					objectId,
					type
				);

				// update batch length and total file length
				item.tailBatch.batchLength += length;
			}

			final void setSourceFile(final AFile file)
			{
				// next source file is set up
				this.currentBatchChannel       =   -1; // invalid value to guarantee change on first entity.
				this.currentSourceFilePosition =    0; // source file starts at 0, of course.
				this.file                      = file;
			}

			final void completeCurrentSourceFile()
			{
				final StorageImportSourceFile.Default[] sourceFileHeads = this.sourceFileHeads;
				final ChannelItem[]                     channelItems    = this.channelItems   ;
				for(int i = 0; i < sourceFileHeads.length; i++)
				{
					final StorageImportSourceFile.Default oldSourceFileHead = sourceFileHeads[i];
					final ChannelItem                     currentItem       = channelItems[i];

					sourceFileHeads[i] = sourceFileHeads[i].next =
						new StorageImportSourceFile.Default(i, this.file, currentItem.headBatch.batchNext)
					;
					currentItem.resetChains();

					// notify storage thread that a new source file is ready for processing
					synchronized(oldSourceFileHead)
					{
						oldSourceFileHead.notifyAll();
					}
				}
			}

		}
		
		static final class ChannelItem
		{
			///////////////////////////////////////////////////////////////////////////
			// instance fields //
			////////////////////
			
			final StorageChannelImportBatch.Default  headBatch  = new StorageChannelImportBatch.Default();
			      StorageChannelImportBatch.Default  tailBatch ;
			      StorageChannelImportEntity.Default tailEntity;
			      
			      
			      
			///////////////////////////////////////////////////////////////////////////
			// methods //
			////////////

			ChannelItem resetChains()
			{
				(this.tailBatch = this.headBatch).next = null;
				this.headBatch.batchNext = null;
				this.tailEntity = null; // gets assigned with the first actual batch
				return this;
			}
			
		}

	}

}
