<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import type { TagResponse, TaskResponse } from '@/lib/api/types';

const props = defineProps<{
  open: boolean;
  task: TaskResponse | null;
  tags: TagResponse[];
  editTitle: string;
  editDescription: string;
  dueDateInput: string | null;
  selectedTagIds: number[];
  newSubtaskTitle: string;
  canAddSubtask: boolean;
  taskModalError: string | null;
  creatingModalTag: boolean;
  modalNewTagName: string;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'update:editTitle', v: string): void;
  (e: 'update:editDescription', v: string): void;
  (e: 'update:dueDateInput', v: string | null): void;
  (e: 'update:selectedTagIds', v: number[]): void;
  (e: 'update:newSubtaskTitle', v: string): void;
  (e: 'update:modalNewTagName', v: string): void;
  (e: 'create-tag-in-modal'): void;
  (e: 'toggle-subtask-completed', subId: number): void;
  (e: 'delete-subtask', subId: number): void;
  (e: 'add-subtask'): void;
  (e: 'save'): void;
  (e: 'delete-task'): void;
}>();

// Local datepicker state derived from props
const showDatePicker = ref(false);
const calendarRef = ref<HTMLElement | null>(null);
const viewYear = ref<number>(new Date().getFullYear());
const viewMonth = ref<number>(new Date().getMonth()); // 0-11
const monthNames = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];

function fmtYmd(d: Date) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function openDatePicker() {
  const current = props.dueDateInput ? new Date(props.dueDateInput) : new Date();
  viewYear.value = current.getFullYear();
  viewMonth.value = current.getMonth();
  showDatePicker.value = true;
}
function closeDatePicker() { showDatePicker.value = false; }
function prevMonth() { if (viewMonth.value === 0) { viewMonth.value = 11; viewYear.value -= 1; } else { viewMonth.value -= 1; } }
function nextMonth() { if (viewMonth.value === 11) { viewMonth.value = 0; viewYear.value += 1; } else { viewMonth.value += 1; } }
function daysInMonth(y: number, m: number) { return new Date(y, m + 1, 0).getDate(); }
function firstWeekday(y: number, m: number) { return new Date(y, m, 1).getDay(); }
function selectDate(day: number) { const d = new Date(viewYear.value, viewMonth.value, day); emit('update:dueDateInput', fmtYmd(d)); closeDatePicker(); }
function clearDate() { emit('update:dueDateInput', null); closeDatePicker(); }

// Map array to set-like toggle
function toggleTagSelection(id: number) {
  const set = new Set(props.selectedTagIds);
  if (set.has(id)) set.delete(id); else set.add(id);
  emit('update:selectedTagIds', Array.from(set));
}

// outside click for calendar
onMounted(() => {
  const onDocClick = (e: MouseEvent) => {
    const target = e.target as Node;
    const calEl = calendarRef.value;
    if (showDatePicker.value && calEl && !calEl.contains(target)) showDatePicker.value = false;
  };
  const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') showDatePicker.value = false; };
  document.addEventListener('click', onDocClick);
  window.addEventListener('keydown', onKey);
  onUnmounted(() => {
    document.removeEventListener('click', onDocClick);
    window.removeEventListener('keydown', onKey);
  });
});

// Lock background scroll when modal is open
const originalOverflow = ref<string | null>(null);
watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      originalOverflow.value = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = originalOverflow.value ?? '';
    }
  },
  { immediate: true }
);

onUnmounted(() => {
  // Ensure body scroll is restored on component unmount
  document.body.style.overflow = originalOverflow.value ?? '';
});
</script>

<template>
  <Transition name="modal-overlay">
  <div v-if="open && task" class="modal-overlay fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <Transition name="modal-content" appear>
  <div class="task-modal-container w-full max-w-5xl max-h-[90vh] overflow-hidden rounded-3xl bg-white/90 backdrop-blur-xl border border-white/20 shadow-2xl flex flex-col">
          <!-- Header -->
          <div class="task-modal-header relative overflow-hidden border-b border-white/20 bg-gradient-to-r from-indigo-50/80 to-purple-50/80 flex-none">
            <div class="absolute inset-0 bg-gradient-to-r from-indigo-400/10 to-purple-500/10"></div>
            <div class="relative p-6">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-4 flex-1 min-w-0">
                  <div class="task-icon p-3 rounded-2xl bg-gradient-to-r from-indigo-500 to-purple-600 shadow-lg flex-shrink-0">
                    <svg class="h-6 w-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"/>
                    </svg>
                  </div>
                  <div class="flex-1 min-w-0">
                    <input
                      :value="editTitle"
                      type="text"
                      class="task-title-input w-full text-xl font-bold bg-white/50 border border-white/30 rounded-2xl px-4 py-3 outline-none focus:border-indigo-400 focus:bg-white/80 transition-all duration-300 backdrop-blur-sm placeholder-gray-500"
                      placeholder="Título da tarefa"
                      @input="emit('update:editTitle', ($event.target as HTMLInputElement).value)"
                    />
                    <div class="flex items-center gap-4 mt-2">
                      <div class="task-meta flex items-center gap-2 text-sm text-gray-600">
                        <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                          <path fill-rule="evenodd" d="M4.083 9h1.946c.089-1.546.383-2.97.837-4.118A6.004 6.004 0 004.083 9zM10 2a8 8 0 100 16 8 8 0 000-16zm0 2c-.076 0-.232.032-.465.262-.238.234-.497.623-.737 1.182-.389.907-.673 2.142-.766 3.556h3.936c-.093-1.414-.377-2.649-.766-3.556-.24-.56-.5-.948-.737-1.182C10.232 4.032 10.076 4 10 4zm3.971 5c-.089-1.546-.383-2.97-.837-4.118A6.004 6.004 0 0115.917 9h-1.946zm-2.003 2H8.032c.093 1.414.377 2.649.766 3.556.24.56.5.948.737 1.182.233.23.389.262.465.262.076 0 .232-.032.465-.262.238-.234.498-.623.737-1.182.389-.907.673-2.142.766-3.556zm1.166 4.118c.454-1.147.748-2.572.837-4.118h1.946a6.004 6.004 0 01-2.783 4.118zm-6.268 0C6.412 13.97 6.118 12.546 6.03 11H4.083a6.004 6.004 0 002.783 4.118z" clip-rule="evenodd"/>
                        </svg>
                        <span>ID #{{ task.id }}</span>
                      </div>
                      <div class="task-status flex items-center gap-2">
                        <div class="status-indicator w-2 h-2 rounded-full" :class="task.completed ? 'bg-green-500' : 'bg-yellow-500'"></div>
                        <span class="text-sm font-medium" :class="task.completed ? 'text-green-700' : 'text-yellow-700'">
                          {{ task.completed ? 'Concluída' : 'Em Andamento' }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
                <button
                  class="close-btn p-3 rounded-xl bg-white/50 border border-white/30 text-gray-600 hover:bg-white/80 hover:text-gray-800 transition-all duration-300 backdrop-blur-sm flex-shrink-0"
                  @click="emit('close')"
                >
                  <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>

            <div class="task-modal-content p-8 overflow-y-auto min-h-0 flex-1">
              <div class="grid gap-8 lg:grid-cols-[2fr_1fr]">
                <div class="main-content space-y-4 sm:space-y-6 md:space-y-8">
                  <section class="subtasks-section">
                    <div class="section-header flex items-center gap-2 sm:gap-3 mb-3 sm:mb-4 md:mb-6">
                      <div class="p-1.5 sm:p-2 rounded-xl bg-gradient-to-r from-blue-500/20 to-indigo-500/20">
                        <svg class="h-3 w-3 sm:h-4 sm:w-4 md:h-5 md:w-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"/>
                        </svg>
                      </div>
                      <div>
                        <h4 class="text-sm sm:text-base md:text-lg font-bold text-gray-800">Subtarefas</h4>
                        <p class="text-xs sm:text-sm text-gray-600">{{ task.subtasks.filter(s => s.completed).length }}/{{ task.subtasks.length }} concluídas</p>
                      </div>
                    </div>

                    <div class="subtasks-list space-y-2 sm:space-y-3 mb-4 sm:mb-6">
                      <div v-for="(st, index) in task.subtasks" :key="st.id" class="subtask-item stagger-item" :style="{ animationDelay: `${index * 100}ms` }">
                        <div class="subtask-card group relative overflow-hidden rounded-xl sm:rounded-2xl bg-white/50 border border-white/30 backdrop-blur-sm p-3 sm:p-4 hover:bg-white/70 transition-all duration-300">
                          <div class="flex items-center gap-3 sm:gap-4">
                            <label :for="`subtask-check-${st.id}`" class="subtask-checkbox relative flex items-center justify-center w-5 h-5 sm:w-6 sm:h-6 cursor-pointer" @click.stop>
                              <input
                                :id="`subtask-check-${st.id}`"
                                type="checkbox"
                                class="sr-only"
                                :checked="st.completed"
                                :aria-label="`Marcar subtarefa ${st.completed ? 'como pendente' : 'como concluída'}`"
                                @change.stop="emit('toggle-subtask-completed', st.id)"
                              />
                              <div class="checkbox-bg absolute inset-0 rounded-lg border-2 transition-all duration-300" :class="st.completed ? 'border-emerald-500 bg-emerald-500' : 'border-gray-300 bg-white group-hover:border-emerald-400'">
                                <svg v-if="st.completed" class="absolute inset-0 w-full h-full text-white transform scale-0 transition-transform duration-300 animate-check-in" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/>
                                </svg>
                              </div>
                            </label>
                            <span class="subtask-title flex-1 text-sm sm:text-base font-medium transition-colors duration-300" :class="st.completed ? 'line-through text-gray-500' : 'text-gray-800 group-hover:text-gray-900'">
                              {{ st.title }}
                            </span>
                            <button
                              class="delete-subtask-btn opacity-0 group-hover:opacity-100 p-1.5 sm:p-2 rounded-lg bg-red-50 text-red-600 hover:bg-red-100 transition-all duration-200"
                              @click="emit('delete-subtask', st.id)"
                              title="Excluir subtarefa"
                            >
                              <svg class="h-3 w-3 sm:h-4 sm:w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                              </svg>
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div class="add-subtask-form rounded-2xl bg-gradient-to-r from-blue-50/50 to-indigo-50/50 border border-white/30 backdrop-blur-sm p-4 sm:p-6">
                      <div class="flex items-center gap-3 mb-4">
                        <div class="p-2 rounded-xl bg-gradient-to-r from-blue-500 to-indigo-600 shadow-lg">
                          <svg class="h-3 w-3 sm:h-4 sm:w-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                          </svg>
                        </div>
                        <h5 class="text-sm sm:text-base font-semibold text-gray-700">Adicionar Subtarefa</h5>
                      </div>
                      <div class="flex flex-col sm:flex-row gap-3">
                        <input
                          :value="newSubtaskTitle"
                          type="text"
                          placeholder="Nova subtarefa"
                          class="modern-input flex-1 rounded-xl border border-white/30 bg-white/50 px-3 sm:px-4 py-3 outline-none backdrop-blur-sm transition-all duration-300 focus:border-blue-400 focus:bg-white/80 focus:shadow-lg placeholder-gray-500 text-sm sm:text-base"
                          @input="emit('update:newSubtaskTitle', ($event.target as HTMLInputElement).value)"
                        />
                        <button
                          :disabled="!newSubtaskTitle.trim() || !canAddSubtask"
                          class="add-subtask-btn group relative overflow-hidden rounded-xl bg-gradient-to-r from-blue-500 to-indigo-600 px-4 sm:px-6 py-3 font-semibold text-white shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-300 disabled:opacity-60 disabled:transform-none text-sm sm:text-base"
                          @click="emit('add-subtask')"
                        >
                          <div class="absolute inset-0 bg-gradient-to-r from-blue-400 to-indigo-500 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                          <div class="relative flex items-center justify-center gap-2">
                            <svg class="h-3 w-3 sm:h-4 sm:w-4 transform group-hover:rotate-90 transition-transform duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                            </svg>
                            <span class="hidden sm:inline">Adicionar</span>
                            <span class="sm:hidden">Add</span>
                          </div>
                        </button>
                      </div>
                      <p class="mt-3 text-xs text-blue-600">
                        <svg class="h-3 w-3 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                        </svg>
                        Máximo de 5 subtarefas por tarefa.
                      </p>
                    </div>
                  </section>

                  <section class="description-section">
                    <div class="section-header flex items-center gap-3 mb-4 sm:mb-6">
                      <div class="p-2 rounded-xl bg-gradient-to-r from-green-500/20 to-emerald-500/20">
                        <svg class="h-4 w-4 sm:h-5 sm:w-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7"/>
                        </svg>
                      </div>
                      <div>
                        <h4 class="text-base sm:text-lg font-bold text-gray-800">Descrição</h4>
                        <p class="text-xs sm:text-sm text-gray-600">Detalhes e contexto da tarefa</p>
                      </div>
                    </div>
                    <textarea
                      :value="editDescription"
                      rows="4"
                      placeholder="Descreva os detalhes da tarefa, contexto, requisitos..."
                      class="modern-input w-full rounded-xl sm:rounded-2xl border border-white/30 bg-white/50 px-4 sm:px-6 py-3 sm:py-4 outline-none backdrop-blur-sm transition-all duration-300 focus:border-green-400 focus:bg-white/80 focus:shadow-lg placeholder-gray-500 resize-none text-sm sm:text-base"
                      @input="emit('update:editDescription', ($event.target as HTMLTextAreaElement).value)"
                    ></textarea>
                  </section>
                </div>

                <div class="sidebar-content space-y-6">
                  <section class="tags-section">
                    <div class="section-card rounded-2xl bg-white/70 border border-white/40 backdrop-blur-sm p-6">
                      <div class="section-header flex items-center gap-3 mb-6">
                        <div class="p-2 rounded-xl bg-gradient-to-r from-pink-500/20 to-rose-500/20">
                          <svg class="h-5 w-5 text-pink-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"/>
                          </svg>
                        </div>
                        <div>
                          <h4 class="font-bold text-gray-800">Tags</h4>
                          <p class="text-sm text-gray-600">Organizar e categorizar</p>
                        </div>
                      </div>

                      <div class="tags-selection flex flex-wrap gap-2 mb-6">
                        <button
                          v-for="tg in tags"
                          :key="tg.id"
                          type="button"
                          class="tag-option group relative overflow-hidden rounded-xl border px-4 py-2 text-sm font-medium transition-all duration-300 hover:scale-105"
                          :class="new Set(selectedTagIds).has(tg.id) ? 'border-pink-400 bg-pink-50 text-pink-700 shadow-lg' : 'border-white/30 bg-white/50 text-gray-700 hover:bg-white/80'"
                          @click="toggleTagSelection(tg.id)"
                        >
                          <div class="flex items-center gap-2">
                            <span class="tag-color w-2.5 h-2.5 rounded-full" :style="{ backgroundColor: tg.color }"></span>
                            {{ tg.name }}
                            <svg v-if="new Set(selectedTagIds).has(tg.id)" class="h-3 w-3 text-pink-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                            </svg>
                          </div>
                        </button>
                      </div>

                      <div class="add-tag-section">
                        <div class="flex gap-2">
                          <input
                            :value="modalNewTagName"
                            type="text"
                            placeholder="Nova tag..."
                            class="modern-input flex-1 rounded-xl border border-white/30 bg-white/50 px-3 py-2 text-sm outline-none backdrop-blur-sm transition-all duration-300 focus:border-pink-400 focus:bg-white/80 placeholder-gray-500"
                            @input="emit('update:modalNewTagName', ($event.target as HTMLInputElement).value)"
                          />
                          <button
                            class="add-tag-btn group relative overflow-hidden rounded-xl bg-gradient-to-r from-pink-500 to-rose-600 px-4 py-2 font-semibold text-white shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-300 disabled:opacity-60 disabled:transform-none"
                            :disabled="creatingModalTag || !modalNewTagName.trim()"
                            @click="emit('create-tag-in-modal')"
                          >
                            <div class="absolute inset-0 bg-gradient-to-r from-pink-400 to-rose-500 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                            <div class="relative">
                              <svg v-if="creatingModalTag" class="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
                                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                              </svg>
                              <svg v-else class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                              </svg>
                            </div>
                          </button>
                        </div>
                        <p class="mt-2 text-xs text-gray-500">Selecione as tags e clique em "Salvar" para aplicar.</p>
                      </div>
                    </div>
                  </section>

                  <section class="due-date-section">
                    <div class="section-card rounded-2xl bg-white/70 border border-white/40 backdrop-blur-sm p-6">
                      <div class="section-header flex items-center gap-3 mb-6">
                        <div class="p-2 rounded-xl bg-gradient-to-r from-orange-500/20 to-amber-500/20">
                          <svg class="h-5 w-5 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                          </svg>
                        </div>
                        <div>
                          <h4 class="font-bold text-gray-800">Data de Vencimento</h4>
                          <p class="text-sm text-gray-600">Prazo para conclusão</p>
                        </div>
                      </div>

                      <div class="relative" ref="calendarRef">
                        <button
                          type="button"
                          class="date-picker-btn group w-full flex items-center justify-between rounded-xl border border-white/30 bg-white/50 px-4 py-3 text-sm hover:bg-white/80 transition-all duration-300 backdrop-blur-sm"
                          @click="openDatePicker"
                        >
                          <span class="flex items-center gap-3">
                            <div class="p-2 rounded-lg bg-orange-500/20 group-hover:bg-orange-500/30 transition-colors duration-300">
                              <svg class="h-4 w-4 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                              </svg>
                            </div>
                            <span class="font-medium">{{ dueDateInput ? dueDateInput : 'Sem data definida' }}</span>
                          </span>
                          <svg class="h-4 w-4 text-gray-400 transform group-hover:rotate-180 transition-transform duration-300" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.172l3.71-3.94a.75.75 0 111.08 1.04l-4.24 4.5a.75.75 0 01-1.08 0l-4.24-4.5a.75.75 0 01.02-1.06z" clip-rule="evenodd"/>
                          </svg>
                        </button>

                        <Transition name="calendar">
                          <div v-if="showDatePicker" class="calendar-picker absolute right-0 z-50 mt-3 w-80 overflow-hidden rounded-2xl border border-white/20 bg-white/90 backdrop-blur-xl shadow-2xl">
                            <div class="calendar-header flex items-center justify-between border-b border-white/20 bg-gradient-to-r from-orange-50/80 to-amber-50/80 px-4 py-3">
                              <button class="nav-btn p-2 rounded-xl hover:bg-white/50 transition-colors duration-200" @click="prevMonth">
                                <svg class="h-4 w-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
                                </svg>
                              </button>
                              <div class="month-year font-semibold text-gray-800">{{ monthNames[viewMonth] }} {{ viewYear }}</div>
                              <button class="nav-btn p-2 rounded-xl hover:bg-white/50 transition-colors duration-200" @click="nextMonth">
                                <svg class="h-4 w-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                                </svg>
                              </button>
                            </div>
                            <div class="calendar-weekdays grid grid-cols-7 gap-1 p-3 text-center text-xs font-semibold text-gray-500 bg-gray-50/50">
                              <div>Dom</div><div>Seg</div><div>Ter</div><div>Qua</div><div>Qui</div><div>Sex</div><div>Sáb</div>
                            </div>
                            <div class="calendar-days grid grid-cols-7 gap-1 p-3">
                              <template v-for="i in ((firstWeekday(viewYear, viewMonth) || 7) - 1)" :key="'empty-'+i">
                                <div class="calendar-day-empty w-10 h-10"></div>
                              </template>
                              <button
                                v-for="d in daysInMonth(viewYear, viewMonth)"
                                :key="'d-'+d"
                                class="calendar-day w-10 h-10 rounded-xl text-sm font-medium transition-all duration-200 hover:bg-orange-100 hover:scale-110"
                                :class="{
                                  'bg-orange-500 text-white hover:bg-orange-500 shadow-lg': dueDateInput === fmtYmd(new Date(viewYear, viewMonth, d)),
                                  'text-gray-700 hover:text-orange-700': dueDateInput !== fmtYmd(new Date(viewYear, viewMonth, d))
                                }"
                                @click="selectDate(d)"
                              >
                                {{ d }}
                              </button>
                            </div>
                            <div class="calendar-actions flex justify-between border-t border-white/20 p-3 bg-gray-50/50">
                              <button class="clear-btn px-4 py-2 rounded-xl text-sm text-gray-600 hover:bg-white/80 transition-colors duration-200" @click="clearDate">Limpar</button>
                              <button class="close-btn px-4 py-2 rounded-xl bg-orange-500/20 text-orange-700 hover:bg-orange-500/30 transition-colors duration-200" @click="closeDatePicker">Fechar</button>
                            </div>
                          </div>
                        </Transition>
                      </div>
                    </div>
                  </section>
                </div>
              </div>
            </div>

            <Transition name="error-fade">
              <div v-if="taskModalError" class="error-message border-t border-red-200/50 bg-red-50/50 px-8 py-4">
                <div class="flex items-center gap-3">
                  <div class="p-2 rounded-lg bg-red-500/20">
                    <svg class="h-4 w-4 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                  </div>
                  <p class="text-sm text-red-700 font-medium">{{ taskModalError }}</p>
                </div>
              </div>
            </Transition>

            <div class="task-modal-footer border-t border-white/20 bg-gradient-to-r from-gray-50/80 to-white/80 backdrop-blur-sm p-6 flex-none">
              <div class="flex items-center justify-between">
                <div class="task-info text-sm text-gray-500">
                  Data: {{ (task.dueDate ? new Date(task.dueDate) : new Date()).toLocaleDateString('pt-BR') }}
                </div>
                <div class="modal-actions flex items-center gap-3">
                  <button
                    class="close-btn px-6 py-3 rounded-xl bg-white/50 border border-white/30 text-gray-700 font-semibold hover:bg-white/80 transition-all duration-300 backdrop-blur-sm"
                    @click="emit('close')"
                  >
                    Fechar
                  </button>
                  <button
                    class="delete-btn group relative overflow-hidden rounded-xl bg-gradient-to-r from-red-500 to-rose-600 px-6 py-3 font-semibold text-white shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-300"
                    @click="emit('delete-task')"
                  >
                    <div class="absolute inset-0 bg-gradient-to-r from-red-400 to-rose-500 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                    <div class="relative flex items-center gap-2">
                      <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                      </svg>
                      Excluir
                    </div>
                  </button>
                  <button
                    class="save-btn group relative overflow-hidden rounded-xl bg-gradient-to-r from-green-500 to-emerald-600 px-8 py-3 font-semibold text-white shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-300"
                    @click="emit('save')"
                  >
                    <div class="absolute inset-0 bg-gradient-to-r from-green-400 to-emerald-500 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                    <div class="relative flex items-center gap-2">
                      <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                      </svg>
                      Salvar Alterações
                    </div>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
</template>
