<script setup lang="ts">
import type { FolderResponse } from '@/lib/api/types';

const props = defineProps<{
  loading: boolean;
  recentIds: number[];
  mine: FolderResponse[];
  participating: FolderResponse[];
  selectedFolderId: number | null;
  allFoldersMap: Map<number, FolderResponse>;
  currentUserId: number | null;
}>();

const emit = defineEmits<{
  (e: 'select-folder', id: number): void;
  (e: 'open-add-folder'): void;
}>();
</script>

<template>
  <aside class="sidebar-glass backdrop-blur-xl bg-white/80 border-r border-white/20 shadow-xl relative overflow-hidden" aria-label="Navegação de pastas">
    <div class="absolute inset-0 bg-gradient-to-b from-white/60 to-transparent pointer-events-none"></div>
    <div class="absolute -top-20 -right-20 w-40 h-40 bg-gradient-to-br from-blue-400/20 to-indigo-500/20 rounded-full blur-3xl animate-float"></div>
    <div class="absolute -bottom-20 -left-20 w-32 h-32 bg-gradient-to-tr from-purple-400/20 to-pink-500/20 rounded-full blur-3xl animate-float-delayed"></div>

    <div class="relative p-6 space-y-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3 min-w-0">
          <div class="p-2.5 rounded-xl bg-gradient-to-r from-blue-500 to-indigo-600 shadow-lg transform hover:scale-105 transition-transform duration-200">
            <svg class="h-6 w-6 text-white" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
              <path d="M10 4H4a2 2 0 00-2 2v12a2 2 0 002 2h16a2 2 0 002-2V8a2 2 0 00-2-2h-8l-2-2z"/>
            </svg>
          </div>
          <h2 class="text-lg font-bold bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-transparent truncate">JTask</h2>
        </div>
        <button
          aria-label="Nova Pasta"
          class="add-folder-btn group relative overflow-hidden rounded-xl bg-gradient-to-r from-blue-500 to-indigo-600 px-3 py-1.5 md:px-4 md:py-2.5 text-xs md:text-sm font-semibold text-white shadow-lg hover:shadow-xl md:transform md:hover:scale-105 transition-all duration-300 shrink-0"
          @click="emit('open-add-folder')"
        >
          <div class="absolute inset-0 bg-gradient-to-r from-blue-400 to-indigo-500 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
          <div class="relative flex items-center gap-2">
            <svg class="h-4 w-4 transform group-hover:rotate-90 transition-transform duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
            </svg>
            <span class="hidden sm:inline">Nova Pasta</span>
          </div>
        </button>
      </div>

      <div v-if="props.loading" class="space-y-6">
        <div class="space-y-3">
          <div class="flex items-center gap-2 mb-4">
            <div class="h-3 w-3 rounded-full bg-gradient-to-r from-blue-300 to-indigo-300 animate-pulse"></div>
            <div class="h-4 w-24 rounded-lg bg-gradient-to-r from-gray-200 to-gray-300 animate-pulse"></div>
          </div>
          <div class="space-y-3">
            <div v-for="i in 3" :key="'sk-recent-'+i" class="skeleton-item h-12 w-full rounded-xl bg-gradient-to-r from-gray-100 to-gray-200 animate-pulse-slow shadow-sm" />
          </div>
        </div>
        <div class="space-y-3">
          <div class="flex items-center gap-2 mb-4">
            <div class="h-3 w-3 rounded-full bg-gradient-to-r from-purple-300 to-pink-300 animate-pulse"></div>
            <div class="h-4 w-20 rounded-lg bg-gradient-to-r from-gray-200 to-gray-300 animate-pulse"></div>
          </div>
          <div class="space-y-3">
            <div v-for="i in 4" :key="'sk-mine-'+i" class="skeleton-item h-12 w-full rounded-xl bg-gradient-to-r from-gray-100 to-gray-200 animate-pulse-slow shadow-sm" />
          </div>
        </div>
      </div>

      <section v-else-if="props.recentIds.length" class="folder-section">
        <div class="flex items-center gap-2 mb-4">
          <div class="p-1.5 rounded-lg bg-gradient-to-r from-orange-400 to-amber-500 shadow-md">
            <svg class="h-3.5 w-3.5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
              <circle cx="12" cy="12" r="9" stroke-width="2"/>
              <path d="M12 7v5l3 3" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <h3 class="text-sm font-bold uppercase tracking-wider text-gray-600">Recentes</h3>
          <div class="flex-1 h-px bg-gradient-to-r from-gray-300 to-transparent"></div>
        </div>
        <ul class="space-y-2">
          <li v-for="(id, index) in props.recentIds" :key="'recent-' + id" class="transform">
            <button
              class="folder-item group relative w-full overflow-hidden rounded-xl p-3 text-left transition-all duration-300 hover:scale-[1.02] hover:shadow-lg"
              :class="props.selectedFolderId === id ? 'folder-item-active' : 'folder-item-inactive'"
              @click="emit('select-folder', id)"
              :style="{ animationDelay: `${index * 100}ms` }"
            >
              <div class="absolute inset-0 bg-gradient-to-r from-white/80 to-white/40 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
              <div class="relative flex items-center gap-3">
                <div class="p-2 rounded-lg bg-gradient-to-r from-orange-400/20 to-amber-500/20 group-hover:from-orange-400/30 group-hover:to-amber-500/30 transition-all duration-300">
                  <svg class="h-4 w-4 text-orange-600 transform group-hover:scale-110 transition-transform duration-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
                    <circle cx="12" cy="12" r="9" stroke-width="1.8"/>
                    <path d="M12 7v5l3 3" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </div>
                <span class="font-semibold text-gray-700 truncate group-hover:text-gray-900 transition-colors duration-300">
                  {{ props.allFoldersMap.get(id)?.name ?? ('Pasta #' + id) }}
                </span>
                <div class="ml-auto opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                  <div class="w-2 h-2 rounded-full bg-gradient-to-r from-blue-400 to-indigo-500 animate-pulse"></div>
                </div>
              </div>
            </button>
          </li>
        </ul>
      </section>

      <section class="folder-section">
        <div class="flex items-center gap-2 mb-4">
          <div class="p-1.5 rounded-lg bg-gradient-to-r from-blue-500 to-indigo-600 shadow-md">
            <svg class="h-3.5 w-3.5 text-white" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
              <path d="M10 4H4a2 2 0 00-2 2v12a2 2 0 002 2h16a2 2 0 002-2V8a2 2 0 00-2-2h-8l-2-2z"/>
            </svg>
          </div>
          <h3 class="text-sm font-bold uppercase tracking-wider text-gray-600">Minhas Pastas</h3>
          <div class="flex-1 h-px bg-gradient-to-r from-gray-300 to-transparent"></div>
          <span class="text-xs font-semibold px-2 py-1 rounded-full bg-blue-100 text-blue-700">{{ props.mine.length }}</span>
        </div>
        <ul class="space-y-2">
          <li v-for="(f, index) in props.mine" :key="f.id" class="transform">
            <button
              class="folder-item group relative w-full overflow-hidden rounded-xl p-3 text-left transition-all duration-300 hover:scale-[1.02] hover:shadow-lg"
              :class="props.selectedFolderId === f.id ? 'folder-item-active' : 'folder-item-inactive'"
              @click="emit('select-folder', f.id)"
              :style="{ animationDelay: `${index * 100}ms` }"
            >
              <div class="absolute inset-0 bg-gradient-to-r from-white/80 to-white/40 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
              <div class="relative flex items-center gap-3">
                <div class="p-2 rounded-lg bg-gradient-to-r from-blue-400/20 to-indigo-500/20 group-hover:from-blue-400/30 group-hover:to-indigo-500/30 transition-all duration-300">
                  <svg class="h-4 w-4 text-blue-600 transform group-hover:scale-110 transition-transform duration-300" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                    <path d="M10 4H4a2 2 0 00-2 2v12a2 2 0 002 2h16a2 2 0 002-2V8a2 2 0 00-2-2h-8l-2-2z"/>
                  </svg>
                </div>
                <span class="font-semibold text-gray-700 truncate group-hover:text-gray-900 transition-colors duration-300">{{ f.name }}</span>
                <div class="ml-auto flex items-center gap-2">
                  <span v-if="props.currentUserId === f.ownerId" class="owner-badge px-2 py-1 rounded-full bg-gradient-to-r from-amber-400 to-orange-500 text-xs font-bold text-white shadow-md transform group-hover:scale-105 transition-transform duration-300">
                    Dono
                  </span>
                  <div class="opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                    <div class="w-2 h-2 rounded-full bg-gradient-to-r from-blue-400 to-indigo-500 animate-pulse"></div>
                  </div>
                </div>
              </div>
            </button>
          </li>
        </ul>
      </section>

      <section class="folder-section">
        <div class="flex items-center gap-2 mb-4">
          <div class="p-1.5 rounded-lg bg-gradient-to-r from-purple-500 to-pink-600 shadow-md">
            <svg class="h-3.5 w-3.5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
            </svg>
          </div>
          <h3 class="text-sm font-bold uppercase tracking-wider text-gray-600">Colaborando</h3>
          <div class="flex-1 h-px bg-gradient-to-r from-gray-300 to-transparent"></div>
          <span class="text-xs font-semibold px-2 py-1 rounded-full bg-purple-100 text-purple-700">{{ props.participating.length }}</span>
        </div>
        <ul class="space-y-2">
          <li v-for="(f, index) in props.participating" :key="f.id" class="transform">
            <button
              class="folder-item group relative w-full overflow-hidden rounded-xl p-3 text-left transition-all duration-300 hover:scale-[1.02] hover:shadow-lg"
              :class="props.selectedFolderId === f.id ? 'folder-item-active' : 'folder-item-inactive'"
              @click="emit('select-folder', f.id)"
              :style="{ animationDelay: `${index * 100}ms` }"
            >
              <div class="absolute inset-0 bg-gradient-to-r from-white/80 to-white/40 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
              <div class="relative flex items-center gap-3">
                <div class="p-2 rounded-lg bg-gradient-to-r from-purple-400/20 to-pink-500/20 group-hover:from-purple-400/30 group-hover:to-pink-500/30 transition-all duration-300">
                  <svg class="h-4 w-4 text-purple-600 transform group-hover:scale-110 transition-transform duration-300" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
                  </svg>
                </div>
                <span class="font-semibold text-gray-700 truncate group-hover:text-gray-900 transition-colors duration-300">{{ f.name }}</span>
                <div class="ml-auto opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                  <div class="w-2 h-2 rounded-full bg-gradient-to-r from-purple-400 to-pink-500 animate-pulse"></div>
                </div>
              </div>
            </button>
          </li>
        </ul>
      </section>
    </div>
  </aside>
</template>
