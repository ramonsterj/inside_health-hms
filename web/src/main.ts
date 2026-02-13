import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { definePreset } from '@primeuix/themes'
import Nora from '@primeuix/themes/nora'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import StyleClass from 'primevue/styleclass'
import Tooltip from 'primevue/tooltip'

import App from './App.vue'
import router from './router'
import i18n from './i18n'
import { initZodI18n } from './validation/zodI18n'

// Initialize Zod with i18n error map for translated validation messages
initZodI18n()

import '@fontsource/lato/300.css'
import '@fontsource/lato/400.css'
import '@fontsource/lato/700.css'
import '@fontsource/lato/900.css'
import '@/assets/tailwind.css'
import '@/assets/styles.scss'

// Custom theme preset with Inside Health brand colors
const InsideHealthTheme = definePreset(Nora, {
  semantic: {
    primary: {
      50: '#f0faf9',
      100: '#d6f1ee',
      200: '#b8e4df',
      300: '#8cd1c9',
      400: '#5ebfb5',
      500: '#3aa99d',
      600: '#2d8a80',
      700: '#266e67',
      800: '#235854',
      900: '#214946',
      950: '#0f2d2b'
    },
    colorScheme: {
      light: {
        primary: {
          color: '#3aa99d',
          contrastColor: '#ffffff',
          hoverColor: '#2d8a80',
          activeColor: '#266e67'
        },
        highlight: {
          background: '#8cd1c9',
          focusBackground: '#5ebfb5',
          color: '#005978',
          focusColor: '#005978'
        }
      },
      dark: {
        primary: {
          color: '#8cd1c9',
          contrastColor: '#005978',
          hoverColor: '#b8e4df',
          activeColor: '#d6f1ee'
        },
        highlight: {
          background: 'rgba(140, 209, 201, 0.2)',
          focusBackground: 'rgba(140, 209, 201, 0.3)',
          color: '#8cd1c9',
          focusColor: '#b8e4df'
        }
      }
    }
  }
})

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)
app.use(PrimeVue, {
  theme: {
    preset: InsideHealthTheme,
    options: {
      darkModeSelector: '.app-dark',
      cssLayer: false
    }
  }
})
app.use(ToastService)
app.use(ConfirmationService)

app.directive('styleclass', StyleClass)
app.directive('tooltip', Tooltip)

app.mount('#app')
