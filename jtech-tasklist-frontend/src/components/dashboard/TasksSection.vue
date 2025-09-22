<script setup lang="ts">
import type { TagResponse, TaskResponse } from '@/lib/api/types';

const props = defineProps<{
  selectedFolder: { id: number; name: string } | null;
  tasks: TaskResponse[];
  tags: TagResponse[];
  tasksLoading: boolean;
  tagMap: Map<number, TagResponse>;
  creatingTask: boolean;
  newTask: { title: string; description: string };
}>();

const emit = defineEmits<{
  (e: 'toggle-completed', task: TaskResponse): void;
  (e: 'open-task', task: TaskResponse): void;
  (e: 'delete-task', id: number): void;
  (e: 'add-task'): void;
  (e: 'update-new-task', payload: Partial<{ title: string; description: string }>): void;
}>();

function onAddTask() {
  emit('add-task');
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <div class="p-3 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 shadow-lg">
          <svg class="h-6 w-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"/>
          </svg>
        </div>
        <div>
          <h2 class="text-2xl font-bold bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-transparent">Tarefas</h2>
          <p class="text-sm text-gray-500">Gerencie suas atividades</p>
        </div>
      </div>
      <div class="task-stats flex items-center gap-4">
        <div class="stats-card px-4 py-2 rounded-xl bg-white/50 border border-white/30 backdrop-blur-sm">
          <span class="text-2xl font-bold text-emerald-600">{{ props.tasks.length }}</span>
          <span class="text-sm text-gray-600 ml-1">total</span>
        </div>
        <div class="stats-card px-4 py-2 rounded-xl bg-white/50 border border-white/30 backdrop-blur-sm">
          <span class="text-2xl font-bold text-blue-600">{{ props.tasks.filter(t => t.completed).length }}</span>
          <span class="text-sm text-gray-600 ml-1">concluídas</span>
        </div>
      </div>
    </div>

    <Transition name="content-fade" mode="out-in">
      <div v-if="props.selectedFolder" key="list" class="space-y-4">
        <div v-if="props.tasksLoading" class="space-y-4">
          <div v-for="i in 4" :key="'tsk-sk-'+i" class="task-skeleton">
            <div class="modern-card rounded-2xl bg-white/60 border border-white/40 p-6 backdrop-blur-sm">
              <div class="flex items-start gap-4">
                <div class="h-5 w-5 rounded-lg bg-gradient-to-r from-gray-200 to-gray-300 animate-pulse"></div>
                <div class="flex-1 space-y-3">
                  <div class="h-5 bg-gradient-to-r from-gray-200 to-gray-300 rounded-lg animate-pulse" style="width: 70%"></div>
                  <div class="h-4 bg-gradient-to-r from-gray-100 to-gray-200 rounded-lg animate-pulse" style="width: 50%"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <template v-else>
          <div v-if="props.tasks.length > 0" class="tasks-grid space-y-4">
            <div v-for="(t, index) in props.tasks" :key="t.id" class="task-card-wrapper" :style="{ animationDelay: `${index * 100}ms` }">
              <div class="task-card group relative overflow-hidden rounded-2xl bg-white/70 border border-white/40 backdrop-blur-sm shadow-lg hover:shadow-2xl transition-all duration-500 hover:scale-[1.02] cursor-pointer" @click="emit('open-task', t)">
                <div class="absolute inset-0 bg-gradient-to-r from-blue-50/30 via-transparent to-purple-50/30 opacity-0 group-hover:opacity-100 transition-opacity duration-500"></div>
                <div class="relative p-6">
                  <div class="flex items-start gap-4">
                    <label :for="`task-check-${t.id}`" class="task-checkbox relative flex items-center justify-center w-6 h-6 cursor-pointer" @click.stop>
                      <input
                        :id="`task-check-${t.id}`"
                        type="checkbox"
                        class="sr-only"
                        :checked="t.completed"
                        :aria-label="`Marcar tarefa ${t.completed ? 'como pendente' : 'como concluída'}`"
                        @change.stop="emit('toggle-completed', t)"
                      />
                      <div class="checkbox-bg absolute inset-0 rounded-lg border-2 transition-all duration-300" :class="t.completed ? 'border-emerald-500 bg-emerald-500' : 'border-gray-300 bg-white group-hover:border-emerald-400'">
                        <svg v-if="t.completed" class="absolute inset-0 w-full h-full text-white transform scale-0 transition-transform duration-300 animate-check-in" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/>
                        </svg>
                      </div>
                    </label>
                    <div class="flex-1 min-w-0">
                      <h3 class="task-title font-semibold text-gray-800 group-hover:text-gray-900 transition-colors duration-300" :class="{ 'line-through text-gray-500': t.completed }">
                        {{ t.title }}
                      </h3>
                      <p v-if="t.description" class="task-description mt-1 text-sm text-gray-600 line-clamp-2" :class="{ 'line-through text-gray-400': t.completed }">
                        {{ t.description }}
                      </p>
                      <div v-if="t.tagIds && t.tagIds.length" class="task-tags flex flex-wrap gap-2 mt-3">
                        <span v-for="tid in t.tagIds.slice(0, 3)" :key="tid" class="tag-pill inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium backdrop-blur-sm border border-white/40 transition-transform duration-200 hover:scale-105" :style="{ backgroundColor: (props.tagMap.get(tid)?.color || '#9ca3af') + '20', borderColor: (props.tagMap.get(tid)?.color || '#9ca3af') + '40', color: props.tagMap.get(tid)?.color || '#6b7280' }">
                          <span class="tag-dot w-2 h-2 rounded-full" :style="{ backgroundColor: props.tagMap.get(tid)?.color || '#9ca3af' }"></span>
                          {{ props.tagMap.get(tid)?.name || 'tag' }}
                        </span>
                        <span v-if="t.tagIds.length > 3" class="more-tags inline-flex items-center rounded-full px-3 py-1 text-xs font-medium bg-gray-100/50 text-gray-600 border border-gray-200/40">
                          +{{ t.tagIds.length - 3 }} mais
                        </span>
                      </div>
                      <div v-if="t.dueDate || (t.subtasks && t.subtasks.length)" class="task-meta flex items-center gap-4 mt-3 text-xs text-gray-500">
                        <div v-if="t.dueDate" class="due-date flex items-center gap-1">
                          <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                          </svg>
                          {{ new Date(t.dueDate).toLocaleDateString('pt-BR') }}
                        </div>
                        <div v-if="t.subtasks && t.subtasks.length" class="subtasks-count flex items-center gap-1">
                          <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"/>
                          </svg>
                          {{ t.subtasks.filter(s => s.completed).length }}/{{ t.subtasks.length }} subtarefas
                        </div>
                      </div>
                    </div>
                    <div class="task-actions flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-all duration-300 transform translate-x-2 group-hover:translate-x-0" @click.stop>
                      <button class="action-btn p-2 rounded-xl bg-blue-500/10 text-blue-600 hover:bg-blue-500/20 hover:scale-110 transition-all duration-200" @click="emit('open-task', t)" title="Ver detalhes">
                        <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                        </svg>
                      </button>
                      <button class="action-btn p-2 rounded-xl bg-red-500/10 text-red-600 hover:bg-red-500/20 hover:scale-110 transition-all duration-200" @click="emit('delete-task', t.id)" title="Excluir">
                        <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                        </svg>
                      </button>
                    </div>
                  </div>
                  <div v-if="t.subtasks && t.subtasks.length" class="progress-section mt-4">
                    <div class="progress-bar h-2 bg-gray-200/50 rounded-full overflow-hidden backdrop-blur-sm">
                      <div class="progress-fill h-full bg-gradient-to-r from-emerald-500 to-teal-600 transition-all duration-500 ease-out" :style="{ width: `${(t.subtasks.filter(s => s.completed).length / t.subtasks.length) * 100}%` }"></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <div class="modern-card rounded-2xl bg-white/40 border border-white/30 backdrop-blur-sm p-12 text-center">
              <div class="empty-icon mx-auto mb-6 p-6 rounded-2xl bg-gradient-to-r from-blue-100 to-indigo-100">
                <svg class="h-12 w-12 mx-auto text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"/>
                </svg>
              </div>
              <h3 class="text-xl font-bold text-gray-600 mb-2">Nenhuma tarefa ainda</h3>
              <p class="text-gray-500 mb-6">Comece criando sua primeira tarefa para organizar seu trabalho.</p>
              <div class="cta-arrow animate-bounce">
                <svg class="h-6 w-6 mx-auto text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3"/>
                </svg>
              </div>
            </div>
          </div>
        </template>

        <div class="add-task-form">
          <div class="modern-card rounded-2xl bg-white/60 border border-white/40 backdrop-blur-sm p-6 hover:shadow-lg transition-all duration-300">
            <div class="flex items-center gap-3 mb-4">
              <div class="p-2 rounded-xl bg-gradient-to-r from-green-500 to-emerald-600 shadow-lg">
                <svg class="h-5 w-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                </svg>
              </div>
              <h3 class="font-semibold text-gray-700">Adicionar Nova Tarefa</h3>
            </div>
            <div class="space-y-4">
              <div class="form-group">
                <input
                  :value="props.newTask.title"
                  @input="emit('update-new-task', { title: ($event.target as HTMLInputElement).value })"
                  type="text"
                  placeholder="Título da tarefa"
                  class="modern-input w-full rounded-xl border border-white/30 bg-white/50 px-4 py-3 outline-none backdrop-blur-sm transition-all duration-300 focus:border-blue-400 focus:bg-white/80 focus:shadow-lg placeholder-gray-500"
                />
              </div>
              <div class="form-group">
                <textarea
                  :value="props.newTask.description"
                  @input="emit('update-new-task', { description: ($event.target as HTMLTextAreaElement).value })"
                  rows="3"
                  placeholder="Descrição detalhada (opcional)"
                  class="modern-input w-full rounded-xl border border-white/30 bg-white/50 px-4 py-3 outline-none backdrop-blur-sm transition-all duration-300 focus:border-blue-400 focus:bg-white/80 focus:shadow-lg placeholder-gray-500 resize-none"
                ></textarea>
              </div>
              <div class="form-actions flex justify-end gap-3">
                <button
                  :disabled="props.creatingTask || !props.newTask.title.trim()"
                  @click="onAddTask"
                  class="add-task-btn group relative overflow-hidden rounded-xl bg-gradient-to-r from-blue-500 to-indigo-600 px-6 py-3 font-semibold text-white shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-300 disabled:opacity-60 disabled:cursor-not-allowed disabled:transform-none"
                >
                  <div class="absolute inset-0 bg-gradient-to-r from-blue-400 to-indigo-500 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                  <div class="relative flex items-center gap-2">
                    <svg v-if="props.creatingTask" class="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <svg v-else class="h-4 w-4 transform group-hover:rotate-90 transition-transform duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                    </svg>
                    <span>{{ props.creatingTask ? 'Criando...' : 'Adicionar Tarefa' }}</span>
                  </div>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-else key="empty" class="empty-folder-state">
        <div class="modern-card rounded-2xl bg-white/40 border border-white/30 backdrop-blur-sm p-16 text-center">
          <div class="empty-folder-icon mx-auto mb-8 p-8 rounded-3xl bg-gradient-to-r from-gray-100 to-gray-200">
            <svg class="h-16 w-16 mx-auto text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M10 4H4a2 2 0 00-2 2v12a2 2 0 002 2h16a2 2 0 002-2V8a2 2 0 00-2-2h-8l-2-2z"/>
            </svg>
          </div>
          <h3 class="text-2xl font-bold text-gray-500 mb-4">Selecione uma pasta</h3>
          <p class="text-gray-400 max-w-md mx-auto">Escolha uma pasta na barra lateral para visualizar e gerenciar suas tarefas.</p>
        </div>
      </div>
    </Transition>
  </div>
</template>
