package fi.helsinki.cs.tmc.intellij.snapshots;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.utilities.JsonMaker;
import fi.helsinki.cs.tmc.intellij.services.ClipboardService;
import fi.helsinki.cs.tmc.intellij.services.PathResolver;
import fi.helsinki.cs.tmc.intellij.services.exercises.CourseAndExerciseManager;
import fi.helsinki.cs.tmc.spyware.*;

import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import name.fraser.neil.plaintext.DiffMatchPatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * When a change in the listened document happens this class creates a diff patch. That created
 * patch is then analyzed and a json is generated from it that is added to the list of items to be
 * sent to the snapshots server.
 */
public class TextInputListener implements DocumentListener {

    private static final Logger logger = LoggerFactory.getLogger(TextInputListener.class);

    private final DiffMatchPatch diff = new DiffMatchPatch();
    private final Project project;
    private String previous;
    private String modified;

    public TextInputListener(Project project) {
        this.project = project;
    }

    @Override
    public void beforeDocumentChange(DocumentEvent documentEvent) {
    }

    @Override
    public void documentChanged(DocumentEvent documentEvent) {
    }

    private boolean isThisCorrectProject() {
        String basePath = project.getBasePath();
        return basePath != null
                && new CourseAndExerciseManager()
                .isCourseInDatabase(
                        PathResolver.getCourseName(basePath));
    }

    private boolean changeIsNotJustWhitespace(DocumentEvent documentEvent) {
        return !documentEvent.getNewFragment().toString().trim().isEmpty()
                || !documentEvent.getOldFragment().toString().trim().isEmpty();
    }

    private void createPatches(Exercise exercise, DocumentEvent documentEvent) {
        List<DiffMatchPatch.Patch> patches;
        patches = diff.patch_make(previous, modified);

        if (isRemoveEvent(documentEvent)) {
            addEventToManager(
                    exercise, "text_remove", generatePatchDescription(documentEvent, patches));
        } else if (isPasteEvent(documentEvent)) {
            addEventToManager(
                    exercise, "text_paste", generatePatchDescription(documentEvent, patches));
        } else {
            addEventToManager(
                    exercise, "text_insert", generatePatchDescription(documentEvent, patches));
        }
    }

    private boolean isPasteEvent(DocumentEvent documentEvent) {
        if (ClipboardService.getClipBoard() == null) {
            return false;
        }
        return (documentEvent.getNewLength() > 2)
                && ClipboardService.getClipBoard()
                        .trim()
                        .equals(documentEvent.getNewFragment().toString().trim());
    }

    private String generatePatchDescription(
            DocumentEvent documentEvent, List<DiffMatchPatch.Patch> patches) {

        logger.info("Creating JSON from patches.");
        VirtualFile file = FileDocumentManager.getInstance().getFile(documentEvent.getDocument());
        if (file == null) {
            return null;
        }
        return JsonMaker.create()
                .add("file", new PathResolver().getPathRelativeToProject(file.getPath()))
                .add("patches", diff.patch_toText(patches))
                .add(
                        "full_document",
                        documentEvent.getNewLength() == documentEvent.getDocument().getTextLength())
                .toString();
    }

    private boolean isRemoveEvent(DocumentEvent documentEvent) {
        return (documentEvent.getOldLength() > 0 && documentEvent.getNewLength() == 0);
    }

    private void addEventToManager(Exercise exercise, String eventType, String text) {
        if (text == null) {
            return;
        }

        LoggableEvent event =
                new LoggableEvent(exercise, eventType, text.getBytes(StandardCharsets.UTF_8));
        SnapshotsEventManager.add(event);
    }
}
