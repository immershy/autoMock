package zw.plugin.mock;

import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import zw.plugin.mock.generator.Generator;
import zw.plugin.mock.generator.impl.BaseDirGenerator;

/**
 * created on 2017/1/14.
 */
public class AutoMockAction extends BaseGenerateAction {

    public AutoMockAction() {
        super(null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Generator generator = new BaseDirGenerator(e);
        generator.generate();
    }
}
