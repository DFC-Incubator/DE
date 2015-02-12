package org.iplantc.de.theme.base.client.diskResource;

import com.google.gwt.i18n.client.Messages;
import com.google.gwt.safehtml.shared.SafeHtml;

import java.util.List;

/**
 * @author jstroot
 */
public interface DiskResourceMessages extends Messages {

    String browse();

    String collapseAll();

    String createDataLinksError();

    String createFolderFailed();

    String createIn(String path);

    SafeHtml dataDragDropStatusText(int size);

    String deleteDataLinksError();

    String deleteFailed();

    String deleteMsg();

    String deleteTrash();

    String diskResourceDoesNotExist(String diskResourcePath);

    String diskResourceIncompleteMove();

    @DefaultMessage("Selected items moved to {0}.")
    @AlternateMessage({"=1", "Selected item moved to {0}."})
    @Key("diskResourceMoveSuccess")
    String diskResourceMoveSuccess(String destPath, @Optional @PluralCount List<String> srcPaths);

    String emptyTrash();

    String emptyTrashWarning();

    @Key("fileFolderDialogHeaderText")
    String fileFolderDialogHeaderText();

    @Key("fileFolderSelectorFieldEmptyText")
    String fileFolderSelectorFieldEmptyText();

    String fileName();

    @Key("fileSelectDialogHeaderText")
    String fileSelectDialogHeaderText();

    String folderName();

    String listDataLinksError();

    String metadataSuccess();

    String metadataUpdateFailed();

    String moveFailed();

    String newFolder();

    String nonDefaultFolderWarning();

    String partialRestore();

    String permissionErrorMessage();

    String permissionSelectErrorMessage();

    String permissions();

    String renameFailed();

    String requiredField();

    String restoreDefaultMsg();

    String restoreMsg();

    String selectAFile();

    String selectAFolder();

    String selectMultipleInputs();

    @Key("selectedFile")
    String selectedFile();

    @Key("selectedFolder")
    String selectedFolder();

    @Key("folderSelectDialogHeaderText")
    String folderSelectDialogHeaderText();

    @Key("newPathListMenuText")
    String newPathListMenuText();

    @Key("selectedItem")
    String selectedItem();

    @Key("share")
    String share();

    @Key("size")
    String size();

    @Key("unsupportedCogeInfoType")
    String unsupportedCogeInfoType();

    @Key("unsupportedEnsemblInfoType")
    String unsupportedEnsemblInfoType();

    @Key("unsupportedTreeInfoType")
    String unsupportedTreeInfoType();
}
