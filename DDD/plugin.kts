import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.findOrCreateDirectory
import liveplugin.*
import java.io.File

registerAction(id = "Pixiu Tools", keyStroke = "ctrl alt P") { event: AnActionEvent ->
    val actionGroup = PopupActionGroup("pixiu 工具",
        AnAction("Execute Shell Command") {
            val command = Messages.showInputDialog("Enter a command (e.g. 'ls'):", "Dialog Title", null)
            if (command != null) show(runShellCommand(command).toString().replace("\n", "<br/>"))
        },
        AnAction("Show Current Project") { popupEvent ->
            // Note that "event" from the outer "Show Actions Popup" action cannot be used here
            // because AnActionEvent objects are not shareable between swing events.
            // The following line might throw "cannot share data context between Swing events"
            // show("project: ${event.project}")

            // Instead, we should use "popupEvent" specific to the "Show current project" action.
            // (In order for "project" to be available in the "popupEvent", createPopup() is called with the "event.dataContext" argument.)
            show("project: ${popupEvent.project}")
        },
        GenerateFileAction(),
        Separator.getInstance(),
        AnAction("Edit Popup...") {
            it.project?.openInEditor("$pluginPath/plugin.kts")
        },
        PopupActionGroup("Documentation",
            AnAction("IntelliJ API Mini-Cheatsheet") {
                openInBrowser("https://github.com/dkandalov/live-plugin/blob/master/IntellijApiCheatSheet.md")
            },
            AnAction("Plugin SDK - Fundamentals") {
                openInBrowser("https://plugins.jetbrains.com/docs/intellij/fundamentals.html")
            }
        )
    )
    actionGroup.createPopup(event.dataContext)
        .showCenteredInCurrentWindow(event.project!!)
}

// 生成 DDD 数据
class GenerateFileAction : AnAction("GenerateFileAction") {

    override fun actionPerformed(e: AnActionEvent) {
        val domain = Messages.showInputDialog("Enter a domain(e.g. 'loan'):", "创建领域", null)
        val project = e.project ?: return
        val basePath = project.basePath ?: return
        val parentDir = LocalFileSystem.getInstance().findFileByIoFile(File(basePath)) ?: return

        // 写入文件
        WriteCommandAction.runWriteCommandAction(project) {
            val domainD = parentDir.findOrCreateDirectory("internal/domain/$domain")
            domainD.createChildData(this, domain + "_service.go")

            val entityD = parentDir.findOrCreateDirectory("internal/domain/$domain/entity")
            entityD.createChildData(this, domain + ".go")

            parentDir.findOrCreateDirectory("internal/domain/$domain/meta")
            parentDir.findOrCreateDirectory("internal/domain/$domain/mysql_repository")
            parentDir.findOrCreateDirectory("internal/domain/$domain/mysql_repository/po")
            parentDir.findOrCreateDirectory("internal/domain/$domain/mysql_repository/factory")
            parentDir.findOrCreateDirectory("internal/domain/$domain/redis_repository")
        }
        show("domain $domain finished!")
    }
}


if (!isIdeStartup) show("Loaded 'Pixiu Tools'<br/>Use ctrl+alt+P to run it")
