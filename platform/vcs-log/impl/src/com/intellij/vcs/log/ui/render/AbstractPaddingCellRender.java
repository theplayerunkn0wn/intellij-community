package com.intellij.vcs.log.ui.render;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vcs.changes.issueLinks.IssueLinkRenderer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.vcs.log.VcsRef;
import com.intellij.vcs.log.data.VcsLogDataHolder;
import com.intellij.vcs.log.graph.PaintInfo;
import com.intellij.vcs.log.graph.render.CommitCell;
import com.intellij.vcs.log.ui.VcsLogColorManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractPaddingCellRender extends ColoredTableCellRenderer {

  @NotNull private final VcsLogDataHolder myDataHolder;

  @NotNull private final RefPainter myRefPainter;
  @NotNull private final IssueLinkRenderer myIssueLinkRenderer;

  @Nullable private PaintInfo myGraphImage;
  @Nullable private Collection<VcsRef> myRefs;

  protected AbstractPaddingCellRender(@NotNull VcsLogColorManager colorManager, @NotNull VcsLogDataHolder dataHolder) {
    myDataHolder = dataHolder;
    myRefPainter = new RefPainter(colorManager, false);
    myIssueLinkRenderer = new IssueLinkRenderer(dataHolder.getProject(), this);
  }

  @Nullable
  protected abstract PaintInfo getGraphImage(int row);

  @Override
  protected void customizeCellRenderer(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    if (value == null) {
      return;
    }

    CommitCell cell = getAssertCommitCell(value);
    myGraphImage = getGraphImage(row);
    myRefs = cell.getRefsToThisCommit();

    int graphPadding = myGraphImage != null ? myGraphImage.getWidth() : 0;
    int textPadding = graphPadding + calcRefsPadding(myRefs);

    append("");
    appendFixedTextFragmentWidth(textPadding);
    myIssueLinkRenderer.appendTextWithLinks(cell.getText());
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    if (myRefs != null) {
      int graphPadding = myGraphImage != null ? myGraphImage.getWidth() : 0;
      drawRefs((Graphics2D)g, myRefs, graphPadding);
    }

    if (myGraphImage != null) {
      UIUtil.drawImage(g, myGraphImage.getImage(), 0, 0, null);
    }
  }

  private static CommitCell getAssertCommitCell(Object value) {
    assert value instanceof CommitCell : "Value of incorrect class was supplied: " + value;
    return (CommitCell)value;
  }

  protected void drawRefs(@NotNull Graphics2D g2, @NotNull Collection<VcsRef> refs, int padding) {
    myRefPainter.drawLabels(g2, collectLabelsForRefs(refs), padding);
  }

  @NotNull
  private Map<String, Color> collectLabelsForRefs(@NotNull Collection<VcsRef> refs) {
    if (refs.isEmpty()) {
      return Collections.emptyMap();
    }
    VirtualFile root = refs.iterator().next().getRoot(); // all refs are from the same commit => they have the same root
    refs = myDataHolder.getLogProvider(root).getReferenceManager().sort(refs);
    List<VcsRef> branches = getBranches(refs);
    Collection<VcsRef> tags = ContainerUtil.subtract(refs, branches);
    return getLabelsForRefs(branches, tags);
  }

  protected int calcRefsPadding(@NotNull Collection<VcsRef> refs) {
    return myRefPainter.padding(collectLabelsForRefs(refs).keySet(), this.getFontMetrics(RefPainter.DEFAULT_FONT));
  }

  @NotNull
  private static Map<String, Color> getLabelsForRefs(@NotNull List<VcsRef> branches, @NotNull Collection<VcsRef> tags) {
    Map<String, Color> labels = ContainerUtil.newLinkedHashMap();
    for (VcsRef branch : branches) {
      labels.put(branch.getName(), branch.getType().getBackgroundColor());
    }
    if (!tags.isEmpty()) {
      VcsRef firstTag = tags.iterator().next();
      Color color = firstTag.getType().getBackgroundColor();
      if (tags.size() > 1) {
        labels.put(firstTag.getName() + " +", color);
      }
      else {
        labels.put(firstTag.getName(), color);
      }
    }
    return labels;
  }

  private static List<VcsRef> getBranches(Collection<VcsRef> refs) {
    return ContainerUtil.filter(refs, new Condition<VcsRef>() {
      @Override
      public boolean value(VcsRef ref) {
        return ref.getType().isBranch();
      }
    });
  }
}
