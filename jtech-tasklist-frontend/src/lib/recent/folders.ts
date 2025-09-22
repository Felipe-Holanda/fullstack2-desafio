const KEY = 'recent_folders';
const MAX = 6;

export function getRecentFolders(): number[] {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return [];
    const arr = JSON.parse(raw) as unknown;
    if (!Array.isArray(arr)) return [];
    return arr.filter((x) => Number.isFinite(x)).map((x) => Number(x)).slice(0, MAX);
  } catch {
    return [];
  }
}

export function pushRecentFolder(id: number) {
  try {
    const current = getRecentFolders();
    const next = [id, ...current.filter((x) => x !== id)].slice(0, MAX);
    localStorage.setItem(KEY, JSON.stringify(next));
  } catch {
    // ignore
  }
}

export function clearRecentFolders() {
  try {
    localStorage.removeItem(KEY);
  } catch {
    // ignore
  }
}
