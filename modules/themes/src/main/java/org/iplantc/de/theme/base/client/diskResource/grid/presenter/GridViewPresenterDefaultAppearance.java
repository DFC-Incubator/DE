package org.iplantc.de.theme.base.client.diskResource.grid.presenter;

import org.iplantc.de.diskResource.client.GridView;
import org.iplantc.de.resources.client.messages.IplantDisplayStrings;
import org.iplantc.de.resources.client.messages.IplantErrorStrings;
import org.iplantc.de.theme.base.client.diskResource.grid.GridViewDisplayStrings;

import com.google.gwt.core.client.GWT;

/**
 * @author jstroot
 */
public class GridViewPresenterDefaultAppearance implements GridView.Presenter.Appearance {
    private final IplantDisplayStrings iplantDisplayStrings;
    private final IplantErrorStrings iplantErrorStrings;
    private final GridViewDisplayStrings displayStrings;

    public GridViewPresenterDefaultAppearance() {
        this(GWT.<IplantDisplayStrings> create(IplantDisplayStrings.class),
             GWT.<IplantErrorStrings> create(IplantErrorStrings.class),
             GWT.<GridViewDisplayStrings> create(GridViewDisplayStrings.class));
    }

    GridViewPresenterDefaultAppearance(final IplantDisplayStrings iplantDisplayStrings,
                                       final IplantErrorStrings iplantErrorStrings,
                                       final GridViewDisplayStrings displayStrings) {
        this.iplantDisplayStrings = iplantDisplayStrings;
        this.iplantErrorStrings = iplantErrorStrings;
        this.displayStrings = displayStrings;
    }

    @Override
    public String comments() {
        return iplantDisplayStrings.comments();
    }

    @Override
    public String commentsDialogHeight() {
        return "450px";
    }

    @Override
    public String commentsDialogWidth() {
        return "600px";
    }

    @Override
    public String copy() {
        return iplantDisplayStrings.copy();
    }

    @Override
    public String copyPasteInstructions() {
        return iplantDisplayStrings.copyPasteInstructions();
    }

    @Override
    public String createDataLinksError() {
        return iplantErrorStrings.createDataLinksError();
    }

    @Override
    public String metadata() {
        return displayStrings.metadata();
    }

    @Override
    public String metadataDialogHeight() {
        return "400";
    }

    @Override
    public String metadataDialogWidth() {
        return "600";
    }

    @Override
    public String metadataFormInvalid() {
        return displayStrings.metadataFormInvalid();
    }

    @Override
    public String metadataHelp() {
        return displayStrings.metadataHelp();
    }

    @Override
    public String shareLinkDialogHeight() {
        return "130";
    }

    @Override
    public int shareLinkDialogTextBoxWidth() {
        return 500;
    }

    @Override
    public String shareLinkDialogWidth() {
        return "535";
    }
}
