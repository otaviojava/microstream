package one.microstream.storage.restclient.app.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.PageConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import one.microstream.storage.restclient.app.SessionData;


@Push
@Theme(value = Lumo.class, variant = Lumo.DARK)
@JsModule("./styles/shared-styles.js")
public class RootLayout extends VerticalLayout 
	implements RouterLayout, BeforeEnterObserver, PageConfigurator
{
	public final static String PAGE_TITLE = "MicroStream Client";
	
	private Component toolBar;
	private Label     headerLabel;
	
	public RootLayout()
	{
		super();

		this.add(this.createBanner());
		this.setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
		this.setMargin(false);
		this.setPadding(false);
		this.setSizeFull();
	}
	
	private Component createBanner()
	{
		this.headerLabel = new Label();
		
		final Button cmdLogout = new Button("Disconnect", event -> {
			this.getUI().ifPresent(ui -> {
				ui.getSession().setAttribute(SessionData.class, null);
				ui.navigate(LoginView.class);
			});
		});
		cmdLogout.setIcon(new Image("images/logout.svg", ""));
		cmdLogout.addThemeVariants(ButtonVariant.LUMO_SMALL);
		
		final HorizontalLayout toolBar = new HorizontalLayout(cmdLogout);
		toolBar.setJustifyContentMode(JustifyContentMode.END);
		this.toolBar = toolBar;
		
		final HorizontalLayout banner = new HorizontalLayout(
			new Image("images/logo.png", "Logo"),
			this.headerLabel,
			UIUtils.compact(toolBar));
		banner.setDefaultVerticalComponentAlignment(Alignment.CENTER);
		banner.setFlexGrow(1, toolBar);
		
		banner.addClassName("banner");
		
		return UIUtils.compact(banner);
	}
	
	@Override
	public void beforeEnter(
		final BeforeEnterEvent event
	)
	{		
		final SessionData sessionData = event.getUI().getSession().getAttribute(SessionData.class);
		this.headerLabel.setText(
			sessionData != null
				? "Client - " + sessionData.baseUrl()
				: "Client"
		);
		this.toolBar.setVisible(
			   sessionData != null
			&& !event.getNavigationTarget().equals(LoginView.class)
		);
	}
	
	@Override
	public void configurePage(
		final InitialPageSettings settings
	)
	{
		settings.addLink("shortcut icon", "images/icon.ico");
		settings.addFavIcon("icon", "images/icon.png", "256x256");
	}
}