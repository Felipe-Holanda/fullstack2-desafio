<script setup lang="ts">
import { reactive, ref, computed, watch } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';

const router = useRouter();
const store = useAuthStore();

const mode = ref<'login' | 'register'>('login');
const loading = ref(false);
const error = ref<string | null>(null);
const showPassword = ref(false);
const pwFocused = ref(false);

const loginForm = reactive({ email: '', password: '' });
const registerForm = reactive({ name: '', email: '', password: '', confirmPassword: '' });

const emailModel = computed({
  get: () => (mode.value === 'login' ? loginForm.email : registerForm.email),
  set: (v: string) => {
    if (mode.value === 'login') loginForm.email = v;
    else registerForm.email = v;
  },
});

const passwordModel = computed({
  get: () => (mode.value === 'login' ? loginForm.password : registerForm.password),
  set: (v: string) => {
    if (mode.value === 'login') loginForm.password = v;
    else registerForm.password = v;
  },
});

const nameValid = computed(() => (mode.value === 'register' ? registerForm.name.trim().length >= 2 : true));
const emailValid = computed(() => /.+@.+\..+/.test(emailModel.value));

const hasUpper = computed(() => /[A-Z]/.test(registerForm.password));
const hasLower = computed(() => /[a-z]/.test(registerForm.password));
const hasNumber = computed(() => /\d/.test(registerForm.password));
const hasSpecial = computed(() => /[^A-Za-z0-9]/.test(registerForm.password));
const hasMinLen = computed(() => registerForm.password.length >= 8);
const requirementsMet = computed(() => [hasUpper.value, hasLower.value, hasNumber.value, hasSpecial.value, hasMinLen.value].every(Boolean));

const passwordValid = computed(() => (mode.value === 'login' ? passwordModel.value.length >= 6 : requirementsMet.value));
const confirmMatches = computed(() => (mode.value === 'register' ? registerForm.password === registerForm.confirmPassword && registerForm.confirmPassword.length > 0 : true));

const strengthScore = computed(() => {
  return [hasUpper.value, hasLower.value, hasNumber.value, hasSpecial.value, hasMinLen.value].filter(Boolean).length;
});
const strengthPercent = computed(() => (strengthScore.value / 5) * 100);
const strengthLabel = computed(() => {
  if (strengthScore.value <= 2) return 'Fraca';
  if (strengthScore.value === 3 || strengthScore.value === 4) return 'Média';
  return 'Forte';
});

const formValid = computed(() => nameValid.value && emailValid.value && passwordValid.value && confirmMatches.value);

watch(mode, () => {
  error.value = null;
});

async function submit() {
  if (!formValid.value || loading.value) return;
  error.value = null;
  loading.value = true;
  try {
    if (mode.value === 'login') {
      await store.login({ email: loginForm.email, password: loginForm.password });
    } else {
      await store.register({ name: registerForm.name.trim(), email: registerForm.email, password: registerForm.password });
      await store.login({ email: registerForm.email, password: registerForm.password });
    }
    router.push('/app');
  } catch (e) {
    const msg = (e as { message?: string } | undefined)?.message || 'Falha ao autenticar';
    error.value = msg;
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="relative flex min-h-[80dvh] items-center justify-center overflow-hidden">
    <div class="pointer-events-none absolute inset-0 -z-10">
      <div class="absolute inset-0 bg-gradient-to-br from-indigo-100 via-white to-sky-100"></div>
      <div class="absolute -top-40 -left-40 h-80 w-80 rounded-full bg-indigo-300/30 blur-3xl"></div>
      <div class="absolute -bottom-40 -right-40 h-80 w-80 rounded-full bg-sky-300/30 blur-3xl"></div>
    </div>

    <div class="mx-4 w-full max-w-md">
      <div class="group relative rounded-2xl border border-white/40 bg-white/70 p-6 shadow-xl backdrop-blur-md">
        <div class="mb-6 flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-semibold tracking-tight">JTask</h1>
            <p class="text-sm text-gray-600">{{ mode === 'login' ? 'Bem-vindo de volta' : 'Crie sua conta' }}</p>
          </div>

          <div class="relative inline-flex select-none rounded-full bg-gray-100 p-1 text-sm">
            <button
              class="relative z-10 min-w-[110px] rounded-full px-4 py-1.5 transition-colors"
              :class="mode === 'login' ? 'text-gray-900' : 'text-gray-500'"
              @click="mode = 'login'"
            >Entrar</button>
            <button
              class="relative z-10 min-w-[110px] rounded-full px-4 py-1.5 transition-colors"
              :class="mode === 'register' ? 'text-gray-900' : 'text-gray-500'"
              @click="mode = 'register'"
            >Cadastrar</button>
            <span
              class="absolute inset-y-1 w-[calc(50%-4px)] rounded-full bg-white shadow transition-transform duration-300"
              :style="{ transform: mode === 'login' ? 'translateX(0)' : 'translateX(100%)' }"
            />
          </div>
        </div>

        <form class="space-y-4" @submit.prevent="submit" novalidate>
          <Transition name="slide-fade" mode="out-in">
            <div v-if="mode === 'register'" key="register" class="space-y-4">
              <div class="relative">
                <label for="name" class="mb-1 block text-sm font-medium">Nome</label>
                <input
                  id="name"
                  v-model.trim="registerForm.name"
                  type="text"
                  autocomplete="name"
                  placeholder="Seu nome"
                  class="peer w-full rounded-xl border border-gray-300/80 bg-white px-4 py-3 shadow-sm outline-none ring-0 transition focus:border-indigo-400 focus:shadow-md"
                  :class="{ 'border-red-400': !nameValid }"
                  required
                  minlength="2"
                />
              </div>
            </div>
            <div v-else key="login-spacer"></div>
          </Transition>

          <div class="relative">
            <label for="email" class="mb-1 block text-sm font-medium">Email</label>
            <div class="relative">
              <input
                id="email"
                v-model="emailModel"
                type="email"
                autocomplete="email"
                placeholder="seuemail@exemplo.com"
                class="peer w-full rounded-xl border border-gray-300/80 bg-white px-4 py-3 pl-11 shadow-sm outline-none ring-0 transition focus:border-indigo-400 focus:shadow-md"
                :class="{ 'border-red-400': !emailValid && (loginForm.email || registerForm.email) }"
                required
              />
              <svg class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 7l8.2 5.47a2 2 0 002.2 0L21 7M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
              </svg>
            </div>
          </div>

          <div class="relative">
            <label for="password" class="mb-1 block text-sm font-medium">Senha</label>
            <div class="relative">
              <input
                id="password"
                v-model="passwordModel"
                :type="showPassword ? 'text' : 'password'"
                :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
                placeholder="********"
                class="peer w-full rounded-xl border border-gray-300/80 bg-white px-4 py-3 pl-11 pr-11 shadow-sm outline-none ring-0 transition focus:border-indigo-400 focus:shadow-md"
                :class="{ 'border-red-400': !passwordValid && passwordModel }"
                required
                minlength="6"
                @focus="pwFocused = true"
                @blur="pwFocused = false"
              />
              <svg class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 11V7a4 4 0 10-8 0v4m-2 0h12v8a2 2 0 01-2 2H6a2 2 0 01-2-2v-8z"/>
              </svg>
              <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700" @click="showPassword = !showPassword" aria-label="Mostrar senha">
                <svg v-if="!showPassword" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z"/>
                  <circle cx="12" cy="12" r="3" stroke-width="1.5" />
                </svg>
                <svg v-else class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 3l18 18M10.58 10.58A3 3 0 0012 15a3 3 0 002.42-4.42M6.82 6.82C3.93 8.5 2 12 2 12s4 8 11 8a10.9 10.9 0 005.18-1.18"/>
                </svg>
              </button>
            </div>
          </div>

          <Transition name="fade">
            <div v-if="mode === 'register' && pwFocused" class="rounded-xl border border-gray-200/80 bg-white px-4 py-3 text-sm shadow-sm">
              <div class="mb-2 flex items-center justify-between">
                <span class="font-medium text-gray-700">Requisitos da senha</span>
                <span class="text-xs text-gray-500">{{ strengthLabel }}</span>
              </div>
              <div class="mb-3 h-2 w-full rounded-full bg-gray-200">
                <div
                  class="h-2 rounded-full bg-gradient-to-r from-red-500 via-yellow-500 to-green-600 transition-all duration-300"
                  :style="{ width: strengthPercent + '%'}"
                />
              </div>
              <ul class="grid grid-cols-1 gap-1 sm:grid-cols-2">
                <li class="flex items-center gap-2" :class="hasUpper ? 'text-green-600' : 'text-gray-600'">
                  <span class="inline-flex h-4 w-4 items-center justify-center">
                    <svg v-if="hasUpper" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                    <svg v-else class="h-4 w-4 text-red-500" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                  </span>
                  Letra maiúscula
                </li>
                <li class="flex items-center gap-2" :class="hasLower ? 'text-green-600' : 'text-gray-600'">
                  <span class="inline-flex h-4 w-4 items-center justify-center">
                    <svg v-if="hasLower" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                    <svg v-else class="h-4 w-4 text-red-500" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                  </span>
                  Letra minúscula
                </li>
                <li class="flex items-center gap-2" :class="hasNumber ? 'text-green-600' : 'text-gray-600'">
                  <span class="inline-flex h-4 w-4 items-center justify-center">
                    <svg v-if="hasNumber" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                    <svg v-else class="h-4 w-4 text-red-500" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                  </span>
                  Número
                </li>
                <li class="flex items-center gap-2" :class="hasSpecial ? 'text-green-600' : 'text-gray-600'">
                  <span class="inline-flex h-4 w-4 items-center justify-center">
                    <svg v-if="hasSpecial" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                    <svg v-else class="h-4 w-4 text-red-500" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                  </span>
                  Caractere especial
                </li>
                <li class="flex items-center gap-2" :class="hasMinLen ? 'text-green-600' : 'text-gray-600'">
                  <span class="inline-flex h-4 w-4 items-center justify-center">
                    <svg v-if="hasMinLen" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                    <svg v-else class="h-4 w-4 text-red-500" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                  </span>
                  Mínimo 8 caracteres
                </li>
              </ul>
            </div>
          </Transition>

          <Transition name="slide-fade" mode="out-in">
            <div v-if="mode === 'register'" key="confirm" class="relative">
              <label for="confirm" class="mb-1 block text-sm font-medium">Confirmar senha</label>
              <input
                id="confirm"
                v-model="registerForm.confirmPassword"
                :type="showPassword ? 'text' : 'password'"
                :autocomplete="'new-password'"
                placeholder="Digite novamente a senha"
                class="w-full rounded-xl border border-gray-300/80 bg-white px-4 py-3 shadow-sm outline-none ring-0 transition focus:border-indigo-400 focus:shadow-md"
                :class="{ 'border-red-400': registerForm.confirmPassword && !confirmMatches }"
                required
                minlength="8"
              />
              <p v-if="registerForm.confirmPassword && !confirmMatches" class="mt-1 text-xs text-red-600">As senhas não coincidem.</p>
            </div>
          </Transition>

          <Transition name="fade">
            <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
          </Transition>

          <button
            :disabled="loading || !formValid"
            type="submit"
            class="group mt-2 w-full rounded-xl bg-gradient-to-r from-indigo-600 to-sky-600 px-5 py-3 font-semibold text-white shadow-md transition hover:from-indigo-500 hover:to-sky-500 hover:shadow-lg disabled:cursor-not-allowed disabled:opacity-60"
          >
            <span class="inline-flex items-center justify-center gap-2">
              <svg v-if="loading" class="h-5 w-5 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <circle cx="12" cy="12" r="10" stroke-opacity="0.25" stroke-width="4"/>
                <path d="M4 12a8 8 0 018-8" stroke-width="4" stroke-linecap="round"/>
              </svg>
              <span>{{ mode === 'login' ? 'Entrar' : 'Cadastrar' }}</span>
            </span>
          </button>
        </form>

        <Transition name="fade">
          <p v-if="mode === 'login'" class="mt-4 text-center text-sm text-gray-600">
            Não tem conta?
            <button class="font-medium text-indigo-600 hover:underline" @click="mode = 'register'">Cadastre-se</button>
          </p>
          <p v-else class="mt-4 text-center text-sm text-gray-600">
            Já é cadastrado?
            <button class="font-medium text-indigo-600 hover:underline" @click="mode = 'login'">Entrar</button>
          </p>
        </Transition>
      </div>
    </div>
  </div>
</template>

<style scoped>
.slide-fade-enter-active,
.slide-fade-leave-active { transition: all 250ms ease; }
.slide-fade-enter-from { opacity: 0; transform: translateY(-6px); }
.slide-fade-leave-to { opacity: 0; transform: translateY(6px); }

.fade-enter-active,
.fade-leave-active { transition: opacity 200ms ease; }
.fade-enter-from,
.fade-leave-to { opacity: 0; }
</style>
