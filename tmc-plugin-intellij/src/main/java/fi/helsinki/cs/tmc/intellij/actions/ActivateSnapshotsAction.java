package fi.helsinki.cs.tmc.intellij.actions;

import fi.helsinki.cs.tmc.intellij.services.PathResolver;
import fi.helsinki.cs.tmc.intellij.services.exercises.CourseAndExerciseManager;
import fi.helsinki.cs.tmc.intellij.snapshots.TextInputListener;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;


/**
 * Adds a snapshot listener to each TMC document when the user starts typing in it.
 */
public class ActivateSnapshotsAction extends TypedHandlerDelegate {

    private static final Set<Document> listenedDocuments =
            Collections.newSetFromMap(new WeakHashMap<>());

    private static final Logger logger = LoggerFactory.getLogger(ActivateSnapshotsAction.class);

    @Override
    public @NotNull Result beforeCharTyped(
            char character,
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull PsiFile file,
            @NotNull FileType fileType) {
        if (!listenedDocuments.contains(editor.getDocument())
                && isThisCorrectProject(project)) {
            DocumentListener docl = new TextInputListener(project);
            editor.getDocument().addDocumentListener(docl, project);
            listenedDocuments.add(editor.getDocument());
            logger.info("Added document listener to {}", editor.getDocument());
        }
        return Result.CONTINUE;
    }

    private boolean isThisCorrectProject(Project project) {
        logger.info("Making sure current exercise should be tracked");
        String basePath = project.getBasePath();
        return basePath != null
                && new CourseAndExerciseManager()
                        .isCourseInDatabase(PathResolver.getCourseName(basePath));
    }
}
