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

import java.util.function.Consumer;

import one.microstream.afs.types.AFile;


public interface StorageImportSourceFile extends StorageClosableFile
{
	public void iterateBatches(Consumer<? super StorageChannelImportBatch> iterator);
	
	
	public static class Default extends StorageChannelFile.Abstract implements StorageImportSourceFile
	{
		final StorageChannelImportBatch.Default headBatch;
	          StorageImportSourceFile.Default   next     ;
	    
	    Default(
			final int                               channelIndex,
			final AFile                             file        ,
			final StorageChannelImportBatch.Default headBatch
		)
		{
			super(file, channelIndex);
			this.headBatch = headBatch;
		}
	    
	    @Override
		public final void iterateBatches(final Consumer<? super StorageChannelImportBatch> iterator)
		{
			for(StorageChannelImportBatch.Default batch = this.headBatch; batch != null; batch = batch.batchNext)
			{
				iterator.accept(batch);
			}
		}

		@Override
		public String toString()
		{
			return Integer.toString(this.channelIndex()) + " "
				+ (this.file() == null ? "<Dummy>"  : this.file().toPathString() + " " + this.headBatch)
			;
		}
		
	}

}
