import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Aura from '@primeuix/themes/aura'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import StyleClass from 'primevue/styleclass'

import App from './App.vue'
import router from './router'
import i18n from './i18n'

import '@/assets/tailwind.css'
import '@/assets/styles.scss'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)
app.use(PrimeVue, {
  theme: {
    preset: Aura,
    options: {
      darkModeSelector: '.app-dark'
    }
  }
})
app.use(ToastService)
app.use(ConfirmationService)

app.directive('styleclass', StyleClass)

app.mount('#app')
