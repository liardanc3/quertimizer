function isTextSelectionActive() {
  const selection = window.getSelection();
  return selection != null && selection.rangeCount > 0 && selection.toString().trim() !== '';
}

function rangeIntersectsNode(range: Range, node: Node) {
  try {
    return range.intersectsNode(node);
  } catch {
    return false;
  }
}

function findSelectionClickScope(target: EventTarget | null) {
  if (!(target instanceof Element)) {
    return null;
  }

  return target.closest('article')
    ?? target.closest('[role="rowgroup"]')
    ?? target.closest('[role="row"]')
    ?? target.closest('button, a, [role="button"]');
}

export function shouldSuppressClickForTextSelection(event: MouseEvent) {
  if (!isTextSelectionActive()) {
    return false;
  }

  const selection = window.getSelection();
  const scope = findSelectionClickScope(event.target);
  if (selection == null || scope == null) {
    return false;
  }

  const range = selection.getRangeAt(0);
  return rangeIntersectsNode(range, scope);
}
