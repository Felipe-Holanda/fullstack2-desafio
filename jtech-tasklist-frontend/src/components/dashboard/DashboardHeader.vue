<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import type { FolderResponse } from '@/lib/api/types';

type Props = {
  selectedFolder: FolderResponse | null;
  isOwner: boolean;
  renamingFolder: boolean;
  renameName: string;
  renamingSaving: boolean;
  currentUserName?: string | null;
  currentUserEmail?: string | null;
};

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'update:renameName', value: string): void;
  (e: 'start-rename'): void;
  (e: 'save-rename'): void;
  (e: 'cancel-rename'): void;
  (e: 'logout'): void;
}>();

const userMenuOpen = ref(false);
const userMenuRef = ref<HTMLElement | null>(null);

onMounted(() => {
  const onDocClick = (e: MouseEvent) => {
    const target = e.target as Node;
    const menuEl = userMenuRef.value;
    if (userMenuOpen.value && menuEl && !menuEl.contains(target)) userMenuOpen.value = false;
  };
  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'Escape') userMenuOpen.value = false;
  };
  document.addEventListener('click', onDocClick);
  window.addEventListener('keydown', onKey);
  onUnmounted(() => {
    document.removeEventListener('click', onDocClick);
    window.removeEventListener('keydown', onKey);
  });
});
</script>

<template>
  <header class="sticky top-0 z-30 backdrop-blur-xl bg-white/80 border-b border-white/20 shadow-lg">
    <div class="flex items-center justify-between p-6">
      <div class="flex items-center gap-4">
        <template v-if="selectedFolder">
          <div class="flex items-center gap-3">
            <div class="p-3 rounded-xl bg-gradient-to-r from-blue-500 to-indigo-600 shadow-lg">
              <svg class="h-6 w-6 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M10 4H4a2 2 0 00-2 2v12a2 2 0 002 2h16a2 2 0 002-2V8a2 2 0 00-2-2h-8l-2-2z"/>
              </svg>
            </div>
            <div>
              <template v-if="isOwner && renamingFolder">
                <div class="flex items-center gap-3">
                  <input
                    :value="renameName"
                    type="text"
                    class="text-xl font-bold bg-white/50 border border-white/30 rounded-xl px-3 py-2 outline-none focus:border-blue-400 focus:bg-white/80 transition-all duration-300 backdrop-blur-sm"
                    @input="emit('update:renameName', ($event.target as HTMLInputElement).value)"
                    @keydown.enter.prevent="emit('save-rename')"
                    @keydown.esc.prevent="emit('cancel-rename')"
                  />
                  <button class="save-btn px-4 py-2 rounded-xl bg-gradient-to-r from-green-500 to-emerald-600 text-white font-semibold shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-300 disabled:opacity-60" :disabled="renamingSaving || !renameName.trim()" @click="emit('save-rename')">
                    <svg v-if="renamingSaving" class="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <span v-else>Salvar</span>
                  </button>
                  <button class="cancel-btn px-4 py-2 rounded-xl bg-white/50 border border-white/30 text-gray-700 font-semibold hover:bg-white/80 transition-all duration-300" @click="emit('cancel-rename')">Cancelar</button>
                </div>
              </template>
              <template v-else>
                <div class="flex items-center gap-3">
                  <h1 class="text-2xl font-bold bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-transparent">{{ selectedFolder.name }}</h1>
                  <button v-if="isOwner" class="edit-btn group p-2 rounded-lg hover:bg-white/50 transition-all duration-200" title="Renomear pasta" @click="emit('start-rename')">
                    <svg class="h-4 w-4 text-gray-500 group-hover:text-blue-600 transition-colors duration-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                    </svg>
                  </button>
                  <span v-if="isOwner" class="owner-badge-header px-3 py-1 rounded-full bg-gradient-to-r from-amber-400 to-orange-500 text-xs font-bold text-white shadow-md">
                    Proprietário
                  </span>
                </div>
              </template>
              <div class="flex items-center gap-4 mt-1">
                <p class="text-sm text-gray-500 flex items-center gap-2">
                  <span class="flex items-center gap-1">
                    <svg class="h-3 w-3" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M4.083 9h1.946c.089-1.546.383-2.97.837-4.118A6.004 6.004 0 004.083 9zM10 2a8 8 0 100 16 8 8 0 000-16zm0 2c-.076 0-.232.032-.465.262-.238.234-.497.623-.737 1.182-.389.907-.673 2.142-.766 3.556h3.936c-.093-1.414-.377-2.649-.766-3.556-.24-.56-.5-.948-.737-1.182C10.232 4.032 10.076 4 10 4zm3.971 5c-.089-1.546-.383-2.97-.837-4.118A6.004 6.004 0 0115.917 9h-1.946zm-2.003 2H8.032c.093 1.414.377 2.649.766 3.556.24.56.5.948.737 1.182.233.23.389.262.465.262.076 0 .232-.032.465-.262.238-.234.498-.623.737-1.182.389-.907.673-2.142.766-3.556zm1.166 4.118c.454-1.147.748-2.572.837-4.118h1.946a6.004 6.004 0 01-2.783 4.118zm-6.268 0C6.412 13.97 6.118 12.546 6.03 11H4.083a6.004 6.004 0 002.783 4.118z" clip-rule="evenodd"/>
                    </svg>
                    ID #{{ selectedFolder.id }}
                  </span>
                  <span class="w-1 h-1 rounded-full bg-gray-400"></span>
                  <span class="flex items-center gap-1">
                    <div class="w-2 h-2 rounded-full" :class="selectedFolder.isPublic ? 'bg-green-500' : 'bg-gray-400'"></div>
                    {{ selectedFolder.isPublic ? 'Pública' : 'Privada' }}
                  </span>
                </p>
              </div>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="flex items-center gap-3">
            <div class="p-3 rounded-xl bg-gradient-to-r from-gray-400 to-gray-500 shadow-lg opacity-50">
              <svg class="h-6 w-6 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M10 4H4a2 2 0 00-2 2v12a2 2 0 002 2h16a2 2 0 002-2V8a2 2 0 00-2-2h-8l-2-2z"/>
              </svg>
            </div>
            <div>
              <h1 class="text-2xl font-bold text-gray-400">Selecione uma pasta</h1>
              <p class="text-sm text-gray-400">Escolha uma pasta na sidebar para começar</p>
            </div>
          </div>
        </template>
      </div>

      <div class="relative flex items-center gap-4" ref="userMenuRef">
        <div class="hidden sm:block text-right">
          <p class="font-semibold text-gray-700">{{ props.currentUserName || 'Usuário' }}</p>
          <p class="text-sm text-gray-500">JTask Dashboard</p>
        </div>
        <button class="user-avatar-btn group relative flex items-center gap-3 rounded-xl bg-white/50 border border-white/30 p-2 pr-3 shadow-lg hover:shadow-xl hover:bg-white/80 transition-all duration-300 backdrop-blur-sm" @click="userMenuOpen = !userMenuOpen" aria-haspopup="menu" :aria-expanded="userMenuOpen">
          <div class="relative">
            <div class="h-10 w-10 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 ring-2 ring-white/60 shadow-lg transform group-hover:scale-105 transition-transform duration-300 flex items-center justify-center">
              <span class="text-white font-bold text-sm">{{ (props.currentUserName || 'U').charAt(0).toUpperCase() }}</span>
            </div>
            <div class="absolute -bottom-1 -right-1 w-3 h-3 bg-green-500 rounded-full border-2 border-white shadow-sm animate-pulse"></div>
          </div>
          <svg class="h-4 w-4 text-gray-500 transition-transform duration-300 group-hover:rotate-180" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.172l3.71-3.94a.75.75 0 111.08 1.04l-4.24 4.5a.75.75 0 01-1.08 0l-4.24-4.5a.75.75 0 01.02-1.06z" clip-rule="evenodd"/>
          </svg>
        </button>

        <Transition name="dropdown">
          <div v-if="userMenuOpen" class="user-dropdown absolute right-0 top-16 z-50 w-64 overflow-hidden rounded-2xl border border-white/20 bg-white/90 backdrop-blur-xl shadow-2xl">
            <div class="p-4 border-b border-white/20 bg-gradient-to-r from-blue-50/50 to-indigo-50/50">
              <div class="flex items-center gap-3">
                <div class="h-12 w-12 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 shadow-lg flex items-center justify-center">
                  <span class="text-white font-bold">{{ (props.currentUserName || 'U').charAt(0).toUpperCase() }}</span>
                </div>
                <div>
                  <p class="font-semibold text-gray-800">{{ props.currentUserName || 'Usuário' }}</p>
                  <p class="text-sm text-gray-600">{{ props.currentUserEmail || 'usuario@example.com' }}</p>
                </div>
              </div>
            </div>
            <div class="p-2">
              <button class="logout-btn group flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left hover:bg-red-50/80 transition-all duration-200" @click="emit('logout')">
                <div class="p-2 rounded-lg bg-red-100 group-hover:bg-red-200 transition-colors duration-200">
                  <svg class="h-4 w-4 text-red-600" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
                  </svg>
                </div>  
                <span class="font-semibold text-red-600">Sair da conta</span>
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </div>
  </header>
</template>
