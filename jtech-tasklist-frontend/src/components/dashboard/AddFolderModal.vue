<script setup lang="ts">
type AddMode = 'create' | 'join';

const props = withDefaults(defineProps<{
  open: boolean;
  addMode: AddMode;
  createName: string;
  createIsPublic: boolean;
  joinKey: string;
}>(), {
  open: false,
  addMode: 'create',
});

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'update:addMode', v: AddMode): void;
  (e: 'update:createName', v: string): void;
  (e: 'update:createIsPublic', v: boolean): void;
  (e: 'update:joinKey', v: string): void;
  (e: 'create'): void;
  (e: 'join'): void;
}>();
</script>

<template>
  <Transition name="modal-overlay">
    <div v-if="props.open" class="modal-overlay fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <Transition name="modal-content" appear>
        <div class="modal-container w-full max-w-lg overflow-hidden rounded-3xl bg-white/90 backdrop-blur-xl border border-white/20 shadow-2xl">
          <div class="modal-header relative overflow-hidden border-b border-white/20 bg-gradient-to-r from-blue-50/80 to-indigo-50/80 px-8 py-6">
            <div class="absolute inset-0 bg-gradient-to-r from-blue-400/10 to-indigo-500/10"></div>
            <div class="relative flex items-center gap-4">
              <div class="modal-icon p-3 rounded-2xl bg-gradient-to-r from-blue-500 to-indigo-600 shadow-lg">
                <svg class="h-6 w-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                </svg>
              </div>
              <div>
                <h3 class="text-xl font-bold bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-transparent">Adicionar Pasta</h3>
                <p class="text-sm text-gray-600">Crie uma nova pasta ou entre em uma existente</p>
              </div>
            </div>
          </div>

          <div class="modal-content p-8 space-y-6">
            <div class="segmented-control relative inline-flex w-full select-none rounded-2xl bg-gray-100/50 p-1.5 backdrop-blur-sm border border-white/30">
              <button
                class="segment-button relative z-10 flex-1 rounded-xl px-6 py-3 text-sm font-semibold transition-all duration-300"
                :class="props.addMode === 'create' ? 'text-blue-700' : 'text-gray-600 hover:text-gray-800'"
                @click="emit('update:addMode', 'create')"
              >
                <div class="flex items-center justify-center gap-2">
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                  </svg>
                  Criar Nova
                </div>
              </button>
              <button
                class="segment-button relative z-10 flex-1 rounded-xl px-6 py-3 text-sm font-semibold transition-all duration-300"
                :class="props.addMode === 'join' ? 'text-blue-700' : 'text-gray-600 hover:text-gray-800'"
                @click="emit('update:addMode', 'join')"
              >
                <div class="flex items-center justify-center gap-2">
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
                  </svg>
                  Entrar Existente
                </div>
              </button>
              <span
                class="segment-indicator absolute inset-y-1.5 w-[calc(50%-6px)] rounded-xl bg-white/80 backdrop-blur-sm shadow-lg border border-white/40 transition-transform duration-300 ease-out"
                :style="{ transform: props.addMode === 'create' ? 'translateX(6px)' : 'translateX(calc(100% + 6px))' }"
              />
            </div>

            <Transition name="form-slide" mode="out-in">
              <div v-if="props.addMode === 'create'" key="create" class="form-section space-y-5">
                <div class="form-group">
                  <label for="create-folder-name" class="form-label block text-sm font-semibold text-gray-700 mb-2">Nome da Pasta</label>
                  <input
                    :value="props.createName"
                    type="text"
                    placeholder="Ex: Projeto JTask, Estudos..."
                    id="create-folder-name"
                    class="modern-input w-full rounded-xl border border-white/30 bg-white/50 px-4 py-3 outline-none backdrop-blur-sm transition-all duration-300 focus:border-blue-400 focus:bg-white/80 focus:shadow-lg placeholder-gray-500"
                    @input="emit('update:createName', ($event.target as HTMLInputElement).value)"
                  />
                </div>
                <div class="form-group">
                  <label class="custom-checkbox inline-flex items-center gap-3 cursor-pointer" for="create-folder-public">
                    <div class="checkbox-container relative">
                      <input id="create-folder-public" type="checkbox" :checked="props.createIsPublic" class="sr-only" @change="emit('update:createIsPublic', ($event.target as HTMLInputElement).checked)" />
                      <div class="checkbox-bg w-5 h-5 rounded-lg border-2 transition-all duration-300" :class="props.createIsPublic ? 'border-blue-500 bg-blue-500' : 'border-gray-300 bg-white'">
                        <svg v-if="props.createIsPublic" class="w-full h-full text-white scale-0 transition-transform duration-300 animate-check-in" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/>
                        </svg>
                      </div>
                    </div>
                    <span class="text-sm font-medium text-gray-700">Pasta pública (outras pessoas podem entrar com código)</span>
                  </label>
                </div>
              </div>
              <div v-else key="join" class="form-section space-y-5">
                <div class="form-group">
                  <label for="join-folder-key" class="form-label block text-sm font-semibold text-gray-700 mb-2">Código da Pasta</label>
                  <input
                    :value="props.joinKey"
                    type="text"
                    placeholder="Cole o código compartilhado aqui"
                    id="join-folder-key"
                    class="modern-input w-full rounded-xl border border-white/30 bg-white/50 px-4 py-3 font-mono outline-none backdrop-blur-sm transition-all duration-300 focus:border-blue-400 focus:bg-white/80 focus:shadow-lg placeholder-gray-500"
                    @input="emit('update:joinKey', ($event.target as HTMLInputElement).value)"
                  />
                </div>
                <div class="info-card rounded-xl bg-blue-50/50 border border-blue-200/50 p-4 backdrop-blur-sm">
                  <div class="flex items-start gap-3">
                    <div class="p-2 rounded-lg bg-blue-500/20">
                      <svg class="h-4 w-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                      </svg>
                    </div>
                    <div>
                      <p class="text-sm font-medium text-blue-800">Como obter o código?</p>
                      <p class="text-xs text-blue-600 mt-1">Peça para o proprietário da pasta compartilhar o código público contigo.</p>
                    </div>
                  </div>
                </div>
              </div>
            </Transition>

            <div class="modal-actions flex items-center justify-end gap-3 pt-4 border-t border-white/20">
              <button
                class="cancel-btn px-6 py-3 rounded-xl bg-white/50 border border-white/30 text-gray-700 font-semibold hover:bg-white/80 transition-all duration-300 backdrop-blur-sm"
                @click="emit('close')"
              >
                Cancelar
              </button>
              <button
                v-if="props.addMode === 'create'"
                class="primary-btn group relative overflow-hidden rounded-xl bg-gradient-to-r from-blue-500 to-indigo-600 px-6 py-3 font-semibold text-white shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-300 disabled:opacity-60 disabled:transform-none"
                :disabled="!props.createName.trim()"
                @click="emit('create')"
              >
                <div class="absolute inset-0 bg-gradient-to-r from-blue-400 to-indigo-500 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                <div class="relative flex items-center gap-2">
                  <svg class="h-4 w-4 transform group-hover:rotate-90 transition-transform duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                  </svg>
                  Criar Pasta
                </div>
              </button>
              <button
                v-else
                class="primary-btn group relative overflow-hidden rounded-xl bg-gradient-to-r from-green-500 to-emerald-600 px-6 py-3 font-semibold text-white shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-300 disabled:opacity-60 disabled:transform-none"
                :disabled="!props.joinKey.trim()"
                @click="emit('join')"
              >
                <div class="absolute inset-0 bg-gradient-to-r from-green-400 to-emerald-500 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                <div class="relative flex items-center gap-2">
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
                  </svg>
                  Entrar na Pasta
                </div>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </Transition>
</template>
