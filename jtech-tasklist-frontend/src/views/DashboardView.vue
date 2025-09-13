<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import foldersService from '@/services/folders';
import tasksService from '@/services/tasks';
import tagsService from '@/services/tags';
import type { FolderMemberResponse, FolderResponse, TagResponse, TaskResponse } from '@/lib/api/types';
import { getRecentFolders, pushRecentFolder } from '@/lib/recent/folders';
import FolderSidebar from '@/components/dashboard/FolderSidebar.vue';
import TasksSection from '@/components/dashboard/TasksSection.vue';
import DashboardHeader from '@/components/dashboard/DashboardHeader.vue';
import AddFolderModal from '@/components/dashboard/AddFolderModal.vue';
import TaskDetailsModal from '@/components/dashboard/TaskDetailsModal.vue';
//

const router = useRouter();
const auth = useAuthStore();

const loading = ref(false);
const error = ref<string | null>(null);

const mine = ref<FolderResponse[]>([]);
const participating = ref<FolderResponse[]>([]);
const recentIds = ref<number[]>(getRecentFolders());
const selectedFolderId = ref<number | null>(null);

const tags = ref<TagResponse[]>([]);
const tasks = ref<TaskResponse[]>([]);
const tasksLoading = ref(false);
const creatingTask = ref(false);
const newTask = reactive({ title: '', description: '' });

// Task details modal
const showTaskModal = ref(false);
const selectedTask = ref<TaskResponse | null>(null);
const selectedTagIds = ref<Set<number>>(new Set());
const newSubtaskTitle = ref('');
const taskModalError = ref<string | null>(null);
// Task edit fields
const editTask = reactive<{ title: string; description: string; dueDateInput: string | null }>({ title: '', description: '', dueDateInput: null });
// Inline tag creation in modal
const modalNewTagName = ref('');
const creatingModalTag = ref(false);

// UI state for modals
const showAddFolder = ref(false);
const addMode = ref<'create' | 'join'>('create');
const createForm = reactive({ name: '', isPublic: false });
const joinForm = reactive({ key: '' });

// User menu (avatar dropdown)
const userMenuOpen = ref(false);
const userMenuRef = ref<HTMLElement | null>(null);

// Folder members & admin
const members = ref<FolderMemberResponse[]>([]);
const membersLoading = ref(false);
const shareCopied = ref(false);
const newTagName = ref('');
const creatingTag = ref(false);
const deletingTagId = ref<number | null>(null);
const makingPublic = ref(false);
// Folder rename state (owner only)
const renamingFolder = ref(false);
const renameName = ref('');
const renamingSaving = ref(false);
// Date picker now handled inside TaskDetailsModal component

// Bridges for child components
const selectedTagIdsArray = computed(() => Array.from(selectedTagIds.value));
function setSelectedTagIdsFromArray(arr: number[]) { selectedTagIds.value = new Set(arr); }

const selectedFolder = computed(() => {
  const id = selectedFolderId.value;
  if (!id) return null;
  return mine.value.concat(participating.value).find((f) => f.id === id) || null;
});

const isOwner = computed(() => {
  const f = selectedFolder.value;
  return !!(f && auth.currentUser && f.ownerId === auth.currentUser.id);
});

const tagMap = computed(() => {
  const m = new Map<number, TagResponse>();
  for (const t of tags.value) m.set(t.id, t);
  return m;
});

const allFoldersMap = computed(() => {
  const m = new Map<number, FolderResponse>();
  for (const f of mine.value) m.set(f.id, f);
  for (const f of participating.value) m.set(f.id, f);
  return m;
});

async function loadFolders() {
  loading.value = true; error.value = null;
  try {
    const [m, p] = await Promise.all([foldersService.listMine(), foldersService.listParticipating()]);
    mine.value = m; participating.value = p;
    // Auto-select: prefer last recent that still exists
    const all = new Map([...m, ...p].map((f) => [f.id, f] as const));
    const recent = recentIds.value.find((id) => all.has(id));
    const pick = recent ?? (m[0]?.id ?? p[0]?.id ?? null);
    if (pick) selectFolder(pick);
  } catch (e) {
    error.value = (e as { message?: string })?.message ?? 'Falha ao carregar pastas';
  } finally {
    loading.value = false;
  }
}

async function selectFolder(id: number) {
  selectedFolderId.value = id;
  pushRecentFolder(id);
  recentIds.value = getRecentFolders();
  await Promise.all([loadTags(id), loadTasks(id), loadMembers(id)]);
}

async function loadTags(folderId: number) {
  try {
    tags.value = await tagsService.list(folderId);
  } catch (e) {
    console.warn('Falha ao carregar tags', e);
  }
}

async function loadTasks(folderId: number) {
  tasksLoading.value = true;
  try {
    tasks.value = await tasksService.list(folderId);
  } catch (e) {
    console.warn('Falha ao carregar tarefas', e);
  } finally {
    tasksLoading.value = false;
  }
}

async function loadMembers(folderId: number) {
  membersLoading.value = true;
  try {
    members.value = await foldersService.listMembers(folderId);
  } catch (e) {
    console.warn('Falha ao carregar membros', e);
  } finally {
    membersLoading.value = false;
  }
}

async function toggleCompleted(task: TaskResponse) {
  if (!selectedFolderId.value) return;
  const prev = task.completed;
  task.completed = !task.completed;
  try {
    await tasksService.setCompleted(selectedFolderId.value, task.id, task.completed);
  } catch {
    task.completed = prev;
  }
}

function openTaskModal(task: TaskResponse) {
  selectedTask.value = task;
  selectedTagIds.value = new Set(task.tagIds || []);
  newSubtaskTitle.value = '';
  taskModalError.value = null;
  showTaskModal.value = true;
  // init edit fields
  editTask.title = task.title;
  editTask.description = task.description || '';
  editTask.dueDateInput = task.dueDate ? task.dueDate.slice(0, 10) : null; // YYYY-MM-DD
}

function closeTaskModal() {
  showTaskModal.value = false;
  selectedTask.value = null;
}

// Tag selection is handled via child component v-model style updates

const canAddSubtask = computed(() => (selectedTask.value ? (selectedTask.value.subtasks?.length || 0) < 5 : false));

// removed saveTaskTags in favor of unified saveTaskEdits

async function addSubtask() {
  if (!selectedFolderId.value || !selectedTask.value) return;
  if (!newSubtaskTitle.value.trim() || !canAddSubtask.value) return;
  try {
    const created = await tasksService.create(selectedFolderId.value, { title: newSubtaskTitle.value.trim(), parentTaskId: selectedTask.value.id });
    selectedTask.value.subtasks.push({ id: created.id, title: created.title, completed: created.completed });
    newSubtaskTitle.value = '';
  } catch (e) {
    taskModalError.value = (e as { message?: string })?.message ?? 'Falha ao adicionar sub-tarefa';
  }
}

async function toggleSubtaskCompleted(sub: { id: number; completed: boolean }) {
  if (!selectedFolderId.value) return;
  const prev = sub.completed;
  sub.completed = !sub.completed;
  try {
    await tasksService.setCompleted(selectedFolderId.value, sub.id, sub.completed);
  } catch {
    sub.completed = prev;
  }
}

function toggleSubtaskById(subId: number) {
  const sub = selectedTask.value?.subtasks.find(s => s.id === subId);
  if (sub) toggleSubtaskCompleted(sub);
}

async function addTask() {
  if (!selectedFolderId.value) return;
  if (!newTask.title.trim()) return;
  creatingTask.value = true;
  try {
    const created = await tasksService.create(selectedFolderId.value, { title: newTask.title.trim(), description: newTask.description || undefined });
    tasks.value.unshift(created);
    newTask.title = '';
    newTask.description = '';
  } catch (e) {
    console.error('Falha ao criar tarefa', e);
  } finally {
    creatingTask.value = false;
  }
}

// Task update/delete actions
async function saveTaskEdits() {
  if (!selectedFolderId.value || !selectedTask.value) return;
  try {
    const updated = await tasksService.update(selectedFolderId.value, selectedTask.value.id, {
      title: editTask.title.trim() || selectedTask.value.title,
      description: editTask.description?.trim() || null,
      dueDate: editTask.dueDateInput ? new Date(editTask.dueDateInput + 'T00:00:00Z').toISOString() : null,
      tagIds: Array.from(selectedTagIds.value),
    });
    // apply to modal and list
    Object.assign(selectedTask.value, updated);
    const idx = tasks.value.findIndex(t => t.id === updated.id);
    if (idx >= 0) tasks.value[idx] = updated;
    taskModalError.value = null;
  } catch (e) {
    taskModalError.value = (e as { message?: string })?.message ?? 'Falha ao salvar alterações';
  }
}

async function deleteTask(taskId?: number) {
  if (!selectedFolderId.value) return;
  const id = taskId ?? selectedTask.value?.id;
  if (!id) return;
  if (!confirm('Tem certeza que deseja excluir esta tarefa?')) return;
  try {
    await tasksService.delete(selectedFolderId.value, id);
    const idx = tasks.value.findIndex(t => t.id === id);
    if (idx >= 0) tasks.value.splice(idx, 1);
    if (selectedTask.value?.id === id) closeTaskModal();
  } catch (e) {
    taskModalError.value = (e as { message?: string })?.message ?? 'Falha ao excluir tarefa';
  }
}

async function deleteSubtask(subId: number) {
  if (!selectedFolderId.value || !selectedTask.value) return;
  try {
    await tasksService.delete(selectedFolderId.value, subId);
    const arr = selectedTask.value.subtasks;
    const idx = arr.findIndex(s => s.id === subId);
    if (idx >= 0) arr.splice(idx, 1);
  } catch (e) {
    taskModalError.value = (e as { message?: string })?.message ?? 'Falha ao excluir subtarefa';
  }
}

// Tags CRUD
async function createTag() {
  if (!selectedFolderId.value) return;
  const name = newTagName.value.trim();
  if (!name) return;
  creatingTag.value = true;
  try {
    const tag = await tagsService.create(selectedFolderId.value, { name });
    tags.value.push(tag);
    newTagName.value = '';
  } catch (e) {
    console.error('Falha ao criar tag', e);
  } finally {
    creatingTag.value = false;
  }
}

async function deleteTag(tagId: number) {
  if (!selectedFolderId.value) return;
  if (!confirm('Excluir esta tag?')) return;
  deletingTagId.value = tagId;
  try {
    await tagsService.delete(selectedFolderId.value, tagId);
    const idx = tags.value.findIndex(t => t.id === tagId);
    if (idx >= 0) tags.value.splice(idx, 1);
    // Optional: clean tasks references
    for (const t of tasks.value) {
      if (t.tagIds?.includes(tagId)) t.tagIds = t.tagIds.filter(id => id !== tagId);
    }
  } catch (e) {
    console.error('Falha ao excluir tag', e);
  } finally {
    deletingTagId.value = null;
  }
}

async function createTagInModal() {
  if (!selectedFolderId.value) return;
  const name = modalNewTagName.value.trim();
  if (!name) return;
  creatingModalTag.value = true;
  try {
    const tag = await tagsService.create(selectedFolderId.value, { name });
    tags.value.push(tag);
    selectedTagIds.value.add(tag.id);
    modalNewTagName.value = '';
  } catch (e) {
    console.error('Falha ao criar tag (modal)', e);
  } finally {
    creatingModalTag.value = false;
  }
}

// Folder admin actions
async function copyKey() {
  const key = selectedFolder.value?.key;
  if (!key) return;
  try {
    await navigator.clipboard.writeText(key);
    shareCopied.value = true;
    setTimeout(() => (shareCopied.value = false), 1500);
  } catch (e) {
    console.error('Falha ao copiar', e);
  }
}

function updateFolderInLists(updated: FolderResponse) {
  const upd = (arr: FolderResponse[]) => {
    const i = arr.findIndex(f => f.id === updated.id);
    if (i >= 0) arr[i] = updated;
  };
  upd(mine.value);
  upd(participating.value);
}

async function rotateKey() {
  const f = selectedFolder.value;
  if (!f) return;
  try {
    const updated = await foldersService.rotateKey(f.id);
    updateFolderInLists(updated);
  } catch (e) {
    console.error('Falha ao rotacionar chave', e);
  }
}

async function saveFolderName() {
  const f = selectedFolder.value;
  if (!f || !isOwner.value) return;
  const name = renameName.value.trim();
  if (!name) return;
  renamingSaving.value = true;
  try {
    const updated = await foldersService.update(f.id, { name });
    updateFolderInLists(updated);
    renamingFolder.value = false;
  } catch (e) {
    console.error('Falha ao renomear pasta', e);
  } finally {
    renamingSaving.value = false;
  }
}

function startRename() {
  if (!selectedFolder.value) return;
  renameName.value = selectedFolder.value.name;
  renamingFolder.value = true;
}

function cancelRename() {
  renamingFolder.value = false;
}

async function deleteFolder() {
  const f = selectedFolder.value;
  if (!f) return;
  if (!confirm('Tem certeza que deseja excluir esta pasta? Esta ação não pode ser desfeita.')) return;
  try {
    await foldersService.delete(f.id);
    // Remove from lists
    const rm = (arr: FolderResponse[]) => {
      const i = arr.findIndex(x => x.id === f.id);
      if (i >= 0) arr.splice(i, 1);
    };
    rm(mine.value);
    rm(participating.value);
    // Pick another folder or clear
    const next = mine.value[0]?.id ?? participating.value[0]?.id ?? null;
    selectedFolderId.value = null;
    if (next) await selectFolder(next);
  } catch (e) {
    console.error('Falha ao excluir pasta', e);
  }
}

async function removeMember(userId: number) {
  const f = selectedFolder.value;
  if (!f) return;
  if (!confirm('Remover este membro da pasta?')) return;
  try {
    await foldersService.removeMember(f.id, userId);
    members.value = members.value.filter(m => m.userId !== userId);
  } catch (e) {
    console.error('Falha ao remover membro', e);
  }
}

async function leaveFolder() {
  const f = selectedFolder.value;
  if (!f || !auth.currentUser) return;
  if (!confirm('Deseja sair desta pasta?')) return;
  try {
    await foldersService.removeMember(f.id, auth.currentUser.id);
    // Remove from participating
    const i = participating.value.findIndex(x => x.id === f.id);
    if (i >= 0) participating.value.splice(i, 1);
    const next = mine.value[0]?.id ?? participating.value[0]?.id ?? null;
    selectedFolderId.value = null;
    if (next) await selectFolder(next);
  } catch (e) {
    console.error('Falha ao sair da pasta', e);
  }
}

// Toggle public (make public)
async function makePublic() {
  const f = selectedFolder.value;
  if (!f || !isOwner.value) return;
  makingPublic.value = true;
  try {
    // update isPublic and rotate key to ensure share code exists
    const updatedBase = await foldersService.update(f.id, { isPublic: true });
    let updated = updatedBase;
    // If the server only sets isPublic and not key, rotate to get key
    if (!updated.key) {
      updated = await foldersService.rotateKey(f.id);
    }
    updateFolderInLists(updated);
  } catch (e) {
    console.error('Falha ao tornar pública', e);
  } finally {
    makingPublic.value = false;
  }
}

// Toggle private (make private)
async function makePrivate() {
  const f = selectedFolder.value;
  if (!f || !isOwner.value) return;
  makingPublic.value = true;
  try {
    const updated = await foldersService.update(f.id, { isPublic: false });
    updateFolderInLists(updated);
  } catch (e) {
    console.error('Falha ao tornar privada', e);
  } finally {
    makingPublic.value = false;
  }
}

async function createFolder() {
  try {
    const f = await foldersService.create({ name: createForm.name.trim(), isPublic: createForm.isPublic });
    mine.value.unshift(f);
    showAddFolder.value = false;
    createForm.name = ''; createForm.isPublic = false;
    await selectFolder(f.id);
  } catch (e) {
    console.error('Falha ao criar pasta', e);
  }
}

async function joinFolder() {
  try {
    const f = await foldersService.join({ key: joinForm.key.trim() });
    participating.value.unshift(f);
    showAddFolder.value = false;
    joinForm.key = '';
    await selectFolder(f.id);
  } catch (e) {
    console.error('Falha ao entrar na pasta', e);
  }
}

function logout() {
  auth.logout();
  router.push('/');
}

onMounted(async () => {
  if (!auth.isAuthenticated) {
    router.push('/');
    return;
  }
  try {
    if (!auth.currentUser) {
      await auth.fetchMe();
    }
  } catch (e) {
    console.warn('Falha ao carregar dados do usuário (me). Efetuando logout.', e);
    auth.logout();
    router.push('/');
    return;
  }

  loadFolders();
  const onDocClick = (e: MouseEvent) => {
    const target = e.target as Node;
    const menuEl = userMenuRef.value;
    if (userMenuOpen.value && menuEl && !menuEl.contains(target)) userMenuOpen.value = false;
  };
  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      userMenuOpen.value = false;
    }
  };
  document.addEventListener('click', onDocClick);
  window.addEventListener('keydown', onKey);
  // cleanup
  onUnmounted(() => {
    document.removeEventListener('click', onDocClick);
    window.removeEventListener('keydown', onKey);
  });
});
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50">
    <div class="grid min-h-screen grid-cols-1 md:grid-cols-[320px_1fr] relative">
      <div class="p-4 md:p-6">
        <FolderSidebar
          class="rounded-2xl overflow-hidden"
          :loading="loading"
          :recent-ids="recentIds"
          :mine="mine"
          :participating="participating"
          :selected-folder-id="selectedFolderId"
          :all-folders-map="allFoldersMap"
          :current-user-id="auth.currentUser?.id ?? null"
          @select-folder="selectFolder"
          @open-add-folder="showAddFolder = true"
        />
      </div>

      <section class="flex-1 min-h-screen">
        <DashboardHeader
          class="rounded-2xl mx-4 md:mx-6 mt-4 md:mt-6"
          :selected-folder="selectedFolder"
          :is-owner="isOwner"
          :renaming-folder="renamingFolder"
          v-model:rename-name="renameName"
          :renaming-saving="renamingSaving"
          :current-user-name="auth.currentUser?.name ?? null"
          :current-user-email="auth.currentUser?.email ?? null"
          @start-rename="startRename"
          @save-rename="saveFolderName"
          @cancel-rename="cancelRename"
          @logout="logout"
        />

        <!-- Main Content Area with Modern Task Cards -->
        <div class="p-6 space-y-6">
          <div class="grid grid-cols-1 xl:grid-cols-[1fr_380px] gap-6">
            <!-- Tasks Section -->
            <TasksSection
              :selected-folder="selectedFolder"
              :tasks="tasks"
              :tags="tags"
              :tasks-loading="tasksLoading"
              :tag-map="tagMap"
              :creating-task="creatingTask"
              :new-task="newTask"
              @toggle-completed="toggleCompleted"
              @open-task="openTaskModal"
              @delete-task="deleteTask"
              @add-task="addTask"
              @update-new-task="(p) => Object.assign(newTask, p)"
            />

            <aside class="sidebar-right space-y-6" aria-label="Tags e ações">
              <section class="tags-section">
                <div class="modern-card rounded-2xl bg-white/70 border border-white/40 backdrop-blur-sm shadow-lg p-6">
                  <div class="flex items-center gap-3 mb-6">
                    <div class="p-3 rounded-xl bg-gradient-to-r from-pink-500 to-rose-600 shadow-lg">
                      <svg class="h-6 w-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"/>
                      </svg>
                    </div>
                    <div>
                      <h2 class="text-xl font-bold bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-transparent">Tags</h2>
                      <p class="text-sm text-gray-500">Organize suas tarefas</p>
                    </div>
                  </div>

                  <div class="tags-list mb-6">
                    <Transition name="tags-fade">
                      <div v-if="tags.length > 0" class="flex flex-wrap gap-3">
                        <Transition name="tag-item" appear v-for="(tag, index) in tags" :key="tag.id">
                          <div class="tag-item group relative overflow-hidden rounded-xl p-3 bg-white/50 border border-white/30 backdrop-blur-sm hover:scale-105 transition-all duration-300 hover:shadow-lg" :style="{ animationDelay: `${index * 100}ms`, backgroundColor: (tag.color + '15') }">
                            <div class="flex items-center gap-3">
                              <div class="tag-color-indicator w-3 h-3 rounded-full shadow-sm" :style="{ backgroundColor: tag.color }"></div>
                              <span class="font-semibold text-gray-700 text-sm">{{ tag.name }}</span>
                              <button
                                class="delete-tag-btn opacity-0 group-hover:opacity-100 p-1 rounded-lg hover:bg-red-100 transition-all duration-200"
                                title="Excluir tag"
                                @click="deleteTag(tag.id)"
                                :disabled="deletingTagId === tag.id"
                              >
                                <svg v-if="deletingTagId === tag.id" class="h-3 w-3 animate-spin text-red-500" fill="none" viewBox="0 0 24 24">
                                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                                <svg v-else class="h-3 w-3 text-red-500 hover:text-red-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                                </svg>
                              </button>
                            </div>
                          </div>
                        </Transition>
                      </div>
                      <div v-else class="empty-tags text-center py-8">
                        <div class="empty-icon mx-auto mb-4 p-4 rounded-2xl bg-gradient-to-r from-pink-100 to-rose-100">
                          <svg class="h-8 w-8 mx-auto text-pink-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"/>
                          </svg>
                        </div>
                        <p class="text-gray-500 text-sm">Nenhuma tag criada ainda</p>
                      </div>
                    </Transition>
                  </div>

                  <div v-if="selectedFolder" class="add-tag-form">
                    <div class="flex gap-3">
                      <input
                        v-model="newTagName"
                        type="text"
                        placeholder="Nome da nova tag"
                        class="modern-input flex-1 rounded-xl border border-white/30 bg-white/50 px-4 py-3 text-sm outline-none backdrop-blur-sm transition-all duration-300 focus:border-pink-400 focus:bg-white/80 focus:shadow-lg placeholder-gray-500"
                      />
                      <button
                        class="add-tag-btn group relative overflow-hidden rounded-xl bg-gradient-to-r from-pink-500 to-rose-600 px-4 py-3 font-semibold text-white shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-300 disabled:opacity-60"
                        @click="createTag"
                        :disabled="creatingTag || !newTagName.trim()"
                      >
                        <div class="absolute inset-0 bg-gradient-to-r from-pink-400 to-rose-500 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                        <div class="relative flex items-center gap-2">
                          <svg v-if="creatingTag" class="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
                            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                          </svg>
                          <svg v-else class="h-4 w-4 transform group-hover:rotate-90 transition-transform duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                          </svg>
                        </div>
                      </button>
                    </div>
                  </div>
                </div>
              </section>

              <section v-if="selectedFolder" class="folder-management">
                <div class="modern-card rounded-2xl bg-white/70 border border-white/40 backdrop-blur-sm shadow-lg p-6">
                  <div class="flex items-center gap-3 mb-6">
                    <div class="p-3 rounded-xl bg-gradient-to-r from-indigo-500 to-purple-600 shadow-lg">
                      <svg class="h-6 w-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                      </svg>
                    </div>
                    <div>
                      <h3 class="text-xl font-bold bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-transparent">Gerenciar Pasta</h3>
                      <p class="text-sm text-gray-500">Configurações e colaboração</p>
                    </div>
                  </div>

                  <div class="visibility-status mb-6">
                    <div class="flex items-center gap-3 p-4 rounded-xl" :class="selectedFolder.isPublic ? 'bg-green-50/50 border border-green-200/50' : 'bg-gray-50/50 border border-gray-200/50'">
                      <div class="p-2 rounded-lg" :class="selectedFolder.isPublic ? 'bg-green-500/20' : 'bg-gray-500/20'">
                        <svg class="h-5 w-5" :class="selectedFolder.isPublic ? 'text-green-600' : 'text-gray-600'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path v-if="selectedFolder.isPublic" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                          <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                        </svg>
                      </div>
                      <div>
                        <p class="font-semibold" :class="selectedFolder.isPublic ? 'text-green-700' : 'text-gray-700'">
                          {{ selectedFolder.isPublic ? 'Pasta Pública' : 'Pasta Privada' }}
                        </p>
                        <p class="text-xs" :class="selectedFolder.isPublic ? 'text-green-600' : 'text-gray-600'">
                          {{ selectedFolder.isPublic ? 'Qualquer pessoa pode entrar com o código' : 'Apenas membros convidados' }}
                        </p>
                      </div>
                    </div>
                  </div>

                  <div v-if="selectedFolder.isPublic" class="sharing-section mb-6">
                    <div class="space-y-4">
                      <label for="share-code-input" class="block text-sm font-semibold text-gray-700">Código de Compartilhamento</label>
                      <div class="share-code-group flex gap-2">
                        <input
                          type="text"
                          :value="selectedFolder.key || ''"
                          readonly
                          id="share-code-input"
                          class="share-code-input flex-1 rounded-xl border border-white/30 bg-white/50 px-4 py-3 font-mono text-sm outline-none backdrop-blur-sm"
                        />
                        <button
                          class="copy-btn group relative overflow-hidden rounded-xl bg-blue-500/20 px-4 py-3 text-blue-700 hover:bg-blue-500/30 transition-all duration-300"
                          @click="copyKey"
                        >
                          <div class="flex items-center gap-2">
                            <svg v-if="!shareCopied" class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/>
                            </svg>
                            <svg v-else class="h-4 w-4 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                            </svg>
                            <span class="text-sm font-semibold">{{ shareCopied ? 'Copiado!' : 'Copiar' }}</span>
                          </div>
                        </button>
                        <button v-if="isOwner" class="rotate-btn group rounded-xl bg-orange-500/20 px-4 py-3 text-orange-700 hover:bg-orange-500/30 transition-all duration-300" @click="rotateKey" title="Gerar novo código">
                          <svg class="h-4 w-4 transform group-hover:rotate-180 transition-transform duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
                          </svg>
                        </button>
                      </div>
                      <div v-if="isOwner" class="privacy-toggle">
                        <button
                          class="make-private-btn group flex items-center gap-2 rounded-xl bg-red-500/20 px-4 py-2 text-red-700 hover:bg-red-500/30 transition-all duration-300"
                          :disabled="makingPublic"
                          @click="makePrivate"
                        >
                          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                          </svg>
                          <span class="text-sm font-semibold">Tornar Privada</span>
                        </button>
                      </div>
                    </div>
                  </div>

                  <div v-else class="privacy-section mb-6">
                    <div class="text-center p-6 rounded-xl bg-gradient-to-r from-gray-50/50 to-slate-50/50 border border-dashed border-gray-300">
                      <div class="lock-icon mx-auto mb-4 p-4 rounded-2xl bg-gradient-to-r from-gray-100 to-gray-200">
                        <svg class="h-8 w-8 mx-auto text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                        </svg>
                      </div>
                      <p class="text-sm text-gray-600 mb-4">Esta pasta é privada e segura.</p>
                      <div v-if="isOwner">
                        <button
                          class="make-public-btn group relative overflow-hidden rounded-xl bg-gradient-to-r from-green-500 to-emerald-600 px-6 py-3 font-semibold text-white shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-300 disabled:opacity-60"
                          :disabled="makingPublic"
                          @click="makePublic"
                        >
                          <div class="absolute inset-0 bg-gradient-to-r from-green-400 to-emerald-500 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                          <div class="relative flex items-center gap-2">
                            <svg v-if="makingPublic" class="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
                              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                            </svg>
                            <svg v-else class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                            </svg>
                            <span>{{ makingPublic ? 'Publicando...' : 'Tornar Pública' }}</span>
                          </div>
                        </button>
                      </div>
                    </div>
                  </div>

                  <div class="members-section">
                    <div class="flex items-center justify-between mb-4">
                      <h4 class="font-bold text-gray-700 flex items-center gap-2">
                        <svg class="h-5 w-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
                        </svg>
                        Membros
                      </h4>
                      <span v-if="membersLoading" class="text-xs text-purple-500 flex items-center gap-1">
                        <svg class="h-3 w-3 animate-spin" fill="none" viewBox="0 0 24 24">
                          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                        carregando...
                      </span>
                      <span v-else class="text-xs font-semibold px-2 py-1 rounded-full bg-purple-100 text-purple-700">{{ members.length }}</span>
                    </div>

                    <div class="members-list space-y-3 max-h-64 overflow-y-auto">
                      <div v-for="(m, index) in members" :key="m.userId" class="member-item" :style="{ animationDelay: `${index * 100}ms` }">
                        <div class="member-card group flex items-center gap-3 rounded-xl bg-white/50 border border-white/30 p-4 backdrop-blur-sm hover:bg-white/70 transition-all duration-300">
                          <div class="member-avatar h-10 w-10 rounded-xl bg-gradient-to-br from-purple-500 to-pink-600 shadow-lg flex items-center justify-center">
                            <span class="text-white font-bold text-sm">{{ m.name.charAt(0).toUpperCase() }}</span>
                          </div>
                          <div class="member-info flex-1 min-w-0">
                            <p class="font-semibold text-gray-800 truncate">{{ m.name }}</p>
                            <p class="text-xs text-gray-600 truncate">{{ m.email }}</p>
                          </div>
                          <div class="member-actions flex items-center gap-2">
                            <button v-if="isOwner && m.userId !== auth.currentUser?.id" class="remove-member-btn group p-2 rounded-lg hover:bg-red-100 transition-all duration-200" @click="removeMember(m.userId)" title="Remover membro">
                              <svg class="h-4 w-4 text-red-500 group-hover:text-red-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                              </svg>
                            </button>
                            <span v-else-if="m.userId === auth.currentUser?.id" class="you-badge px-2 py-1 rounded-lg bg-blue-100 text-blue-700 text-xs font-semibold">Você</span>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div class="folder-actions flex gap-3 mt-6 pt-6 border-t border-white/30">
                      <button v-if="isOwner" class="delete-folder-btn group flex-1 flex items-center justify-center gap-2 rounded-xl bg-red-500/20 px-4 py-3 text-red-700 hover:bg-red-500/30 transition-all duration-300" @click="deleteFolder">
                        <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                        </svg>
                        <span class="text-sm font-semibold">Excluir Pasta</span>
                      </button>
                      <button v-else class="leave-folder-btn group flex-1 flex items-center justify-center gap-2 rounded-xl bg-orange-500/20 px-4 py-3 text-orange-700 hover:bg-orange-500/30 transition-all duration-300" @click="leaveFolder">
                        <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
                        </svg>
                        <span class="text-sm font-semibold">Sair da Pasta</span>
                      </button>
                    </div>
                  </div>
                </div>
              </section>
            </aside>
          </div>
        </div>
      </section>
    </div>

    <AddFolderModal
      :open="showAddFolder"
      :add-mode="addMode"
      :create-name="createForm.name"
      :create-is-public="createForm.isPublic"
      :join-key="joinForm.key"
      @close="showAddFolder = false"
      @update:addMode="(m) => (addMode = m)"
      @update:createName="(v) => (createForm.name = v)"
      @update:createIsPublic="(v) => (createForm.isPublic = v)"
      @update:joinKey="(v) => (joinForm.key = v)"
      @create="createFolder"
      @join="joinFolder"
    />

    <TaskDetailsModal
      :open="showTaskModal"
      :task="selectedTask"
      :tags="tags"
      :edit-title="editTask.title"
      :edit-description="editTask.description"
      :due-date-input="editTask.dueDateInput"
      :selected-tag-ids="selectedTagIdsArray"
      :new-subtask-title="newSubtaskTitle"
      :can-add-subtask="canAddSubtask"
      :task-modal-error="taskModalError"
      :creating-modal-tag="creatingModalTag"
      :modal-new-tag-name="modalNewTagName"
      @close="closeTaskModal"
      @update:editTitle="(v) => (editTask.title = v)"
      @update:editDescription="(v) => (editTask.description = v)"
      @update:dueDateInput="(v) => (editTask.dueDateInput = v)"
      @update:selectedTagIds="setSelectedTagIdsFromArray"
      @update:newSubtaskTitle="(v) => (newSubtaskTitle = v)"
      @update:modalNewTagName="(v) => (modalNewTagName = v)"
      @create-tag-in-modal="createTagInModal"
      @toggle-subtask-completed="toggleSubtaskById"
      @delete-subtask="deleteSubtask"
      @add-subtask="addSubtask"
      @save="saveTaskEdits"
      @delete-task="deleteTask()"
    />
  </div>
</template>

<style>
:root {
  --glass-bg: rgba(255, 255, 255, 0.7);
  --glass-border: rgba(255, 255, 255, 0.2);
  --shadow-soft: 0 8px 32px rgba(0, 0, 0, 0.1);
  --shadow-strong: 0 20px 64px rgba(0, 0, 0, 0.15);
  --gradient-primary: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  --gradient-secondary: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  --gradient-success: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  --transition-smooth: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-spring: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes float {
  0%, 100% { transform: translateY(0px) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(3deg); }
}

@keyframes float-delayed {
  0%, 100% { transform: translateY(0px) rotate(0deg); }
  50% { transform: translateY(-15px) rotate(-2deg); }
}

@keyframes pulse-slow {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.8; }
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes checkIn {
  from {
    opacity: 0;
    transform: scale(0);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.animate-float { animation: float 6s ease-in-out infinite; }
.animate-float-delayed { animation: float-delayed 8s ease-in-out infinite; }
.animate-pulse-slow { animation: pulse-slow 3s ease-in-out infinite; }
.animate-check-in { animation: checkIn 0.3s ease-out; }

.sidebar-glass {
  background: linear-gradient(145deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(255, 255, 255, 0.85) 50%,
    rgba(255, 255, 255, 0.75) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow:
    0 8px 32px rgba(31, 38, 135, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
}

.modern-card {
  background: linear-gradient(145deg,
    rgba(255, 255, 255, 0.8) 0%,
    rgba(255, 255, 255, 0.6) 100%);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow:
    0 8px 32px rgba(31, 38, 135, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  transition: var(--transition-smooth);
}

.modern-card:hover {
  box-shadow:
    0 16px 48px rgba(31, 38, 135, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  transform: translateY(-2px);
}

/* Enhanced Folder Items */
.folder-item {
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.6), rgba(255, 255, 255, 0.4));
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 16px rgba(31, 38, 135, 0.08);
  transition: var(--transition-spring);
  animation: slideInLeft 0.6s ease-out;
  animation-fill-mode: both;
}

.folder-item-active {
  background: linear-gradient(145deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(99, 102, 241, 0.15) 100%);
  border: 1px solid rgba(59, 130, 246, 0.3);
  box-shadow:
    0 8px 24px rgba(59, 130, 246, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.folder-item-inactive {
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.6), rgba(255, 255, 255, 0.4));
}

.folder-item:hover {
  transform: translateX(4px) scale(1.02);
  box-shadow: 0 12px 32px rgba(31, 38, 135, 0.15);
}

/* Enhanced Buttons */
.add-folder-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
  position: relative;
  overflow: hidden;
}

.add-folder-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.5s;
}

.add-folder-btn:hover::before {
  left: 100%;
}

/* Enhanced Task Cards */
.task-card {
  background: linear-gradient(145deg,
    rgba(255, 255, 255, 0.9) 0%,
    rgba(255, 255, 255, 0.7) 100%);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow:
    0 8px 32px rgba(31, 38, 135, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  transition: all 0.5s cubic-bezier(0.165, 0.84, 0.44, 1);
  position: relative;
  overflow: hidden;
}

.task-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg,
    transparent,
    rgba(255, 255, 255, 0.3),
    transparent);
  transition: left 0.6s ease;
}

.task-card:hover::before {
  left: 100%;
}

.task-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow:
    0 24px 64px rgba(31, 38, 135, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.task-card-wrapper {
  animation: slideInUp 0.6s ease-out;
  animation-fill-mode: both;
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes modalSlideOut {
  from {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
  to {
    opacity: 0;
    transform: scale(0.95) translateY(20px);
  }
}

@keyframes overlayFadeIn {
  from {
    opacity: 0;
    backdrop-filter: blur(0px);
  }
  to {
    opacity: 1;
    backdrop-filter: blur(4px);
  }
}

@keyframes overlayFadeOut {
  from {
    opacity: 1;
    backdrop-filter: blur(4px);
  }
  to {
    opacity: 0;
    backdrop-filter: blur(0px);
  }
}

@keyframes subtaskSlideIn {
  from {
    opacity: 0;
    transform: translateX(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

@keyframes calendarSlideDown {
  from {
    opacity: 0;
    transform: translateY(-10px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes calendarSlideUp {
  from {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateY(-10px) scale(0.95);
  }
}

@keyframes errorSlideDown {
  from {
    opacity: 0;
    transform: translateY(-100%);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes errorSlideUp {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(-100%);
  }
}

@keyframes checkboxBounce {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.modal-overlay-enter-active {
  animation: overlayFadeIn 0.3s ease-out;
}

.modal-overlay-leave-active {
  animation: overlayFadeOut 0.3s ease-in;
}

.modal-content-enter-active {
  animation: modalSlideIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.modal-content-leave-active {
  animation: modalSlideOut 0.3s ease-in;
}

.calendar-enter-active {
  animation: calendarSlideDown 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.calendar-leave-active {
  animation: calendarSlideUp 0.2s ease-in;
}

.subtask-list-enter-active {
  animation: subtaskSlideIn 0.4s ease-out;
}

.error-fade-enter-active {
  animation: errorSlideDown 0.3s ease-out;
}

.error-fade-leave-active {
  animation: errorSlideUp 0.3s ease-in;
}

/* Enhanced Modal Styles */
.task-modal-container {
  position: relative;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(145deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(255, 255, 255, 0.85) 100%);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow:
    0 25px 50px rgba(0, 0, 0, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
}

.task-modal-content {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(156, 163, 175, 0.5) transparent;
}

.task-modal-content::-webkit-scrollbar {
  width: 8px;
}

.task-modal-content::-webkit-scrollbar-track {
  background: rgba(156, 163, 175, 0.1);
  border-radius: 4px;
}

.task-modal-content::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.5);
  border-radius: 4px;
}

.task-modal-content::-webkit-scrollbar-thumb:hover {
  background: rgba(156, 163, 175, 0.7);
}

.subtask-checkbox input:checked + .checkbox-bg {
  animation: checkboxBounce 0.3s ease-out;
}

.subtask-checkbox input:checked + .checkbox-bg svg {
  animation: checkIn 0.4s ease-out;
}

.section-card {
  position: relative;
  overflow: hidden;
}

.section-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  transition: left 0.5s ease;
}

.section-card:hover::before {
  left: 100%;
}

.task-checkbox .checkbox-bg {
  background: linear-gradient(145deg, #ffffff, #f8fafc);
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.task-checkbox input:checked + .checkbox-bg {
  background: linear-gradient(135deg, #10b981, #059669);
  box-shadow:
    0 4px 16px rgba(16, 185, 129, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

.progress-bar {
  background: linear-gradient(90deg,
    rgba(229, 231, 235, 0.6),
    rgba(209, 213, 219, 0.6));
  backdrop-filter: blur(8px);
  position: relative;
  overflow: hidden;
}

.progress-fill {
  background: linear-gradient(90deg, #10b981, #059669, #047857);
  position: relative;
  border-radius: inherit;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  right: 0;
  background-image: linear-gradient(
    -45deg,
    rgba(255, 255, 255, 0.2) 25%,
    transparent 25%,
    transparent 50%,
    rgba(255, 255, 255, 0.2) 50%,
    rgba(255, 255, 255, 0.2) 75%,
    transparent 75%,
    transparent
  );
  background-size: 30px 30px;
  animation: shimmer 2s linear infinite;
}

.tag-pill {
  backdrop-filter: blur(12px);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.tag-pill::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.4s ease;
}

.tag-pill:hover::before {
  left: 100%;
}

.tag-dot {
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.3);
  transition: all 0.2s ease;
}

.tag-pill:hover .tag-dot {
  transform: scale(1.2);
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.5);
}

.modern-input {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.modern-input:focus {
  transform: translateY(-1px);
  box-shadow:
    0 12px 32px rgba(59, 130, 246, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
}

.user-avatar-btn {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.user-avatar-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 48px rgba(31, 38, 135, 0.2);
}

.dropdown-enter-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.dropdown-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 1, 1);
}
.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.98);
}

.content-fade-enter-active,
.content-fade-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.content-fade-enter-from {
  opacity: 0;
  transform: translateY(20px);
}
.content-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.stagger-fade-enter-active {
  transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}
.stagger-fade-enter-from {
  opacity: 0;
  transform: translateY(30px) scale(0.95);
}

.skeleton-item {
  position: relative;
  overflow: hidden;
  background: linear-gradient(90deg,
    rgba(229, 231, 235, 0.6),
    rgba(209, 213, 219, 0.8),
    rgba(229, 231, 235, 0.6));
  background-size: 200% 100%;
  animation: shimmer 2s infinite ease-in-out;
}

.action-btn {
  backdrop-filter: blur(8px);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.action-btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  transition: all 0.3s ease;
  transform: translate(-50%, -50%);
}

.action-btn:hover::before {
  width: 100px;
  height: 100px;
}

.stats-card {
  backdrop-filter: blur(16px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.stats-card:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 12px 32px rgba(31, 38, 135, 0.15);
}

.empty-state,
.empty-folder-state {
  animation: scaleIn 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.empty-icon,
.empty-folder-icon {
  position: relative;
}

.empty-icon::after,
.empty-folder-icon::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 120%;
  height: 120%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  animation: pulse-slow 3s ease-in-out infinite;
}

.cta-arrow {
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 20%, 53%, 80%, 100% {
    transform: translate3d(0, 0, 0);
  }
  40%, 43% {
    transform: translate3d(0, -8px, 0);
  }
  70% {
    transform: translate3d(0, -4px, 0);
  }
  90% {
    transform: translate3d(0, -2px, 0);
  }
}

.member-item {
  animation: slideInUp 0.5s ease-out;
  animation-fill-mode: both;
}

.member-card {
  backdrop-filter: blur(12px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.member-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.member-card:hover::before {
  left: 100%;
}

.owner-badge,
.owner-badge-header,
.you-badge {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
  position: relative;
  overflow: hidden;
}

.owner-badge::before,
.owner-badge-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  animation: shimmer 2s infinite ease;
}

@media (max-width: 768px) {
  .sidebar-glass {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 50;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }

  .sidebar-glass.open {
    transform: translateX(0);
  }

  .task-card {
    margin-bottom: 1rem;
  }

  .task-actions {
    opacity: 1;
    transform: translateX(0);
  }
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.fade-enter-active, .fade-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.fade-enter-from {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}
.fade-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.95);
}

.tags-fade-enter-active,
.tags-fade-leave-active {
  transition: all 0.3s ease;
}
.tags-fade-enter-from,
.tags-fade-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

.tag-item-enter-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.tag-item-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.8);
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}
.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
