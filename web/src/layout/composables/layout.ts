import { computed, reactive } from 'vue'

/** Breakpoint (in pixels) below which the layout switches to mobile mode */
const DESKTOP_BREAKPOINT = 991

export interface LayoutConfig {
  preset: string
  primary: string
  surface: string | null
  darkTheme: boolean
  menuMode: 'static' | 'overlay'
}

export interface LayoutState {
  staticMenuInactive: boolean
  overlayMenuActive: boolean
  profileSidebarVisible: boolean
  configSidebarVisible: boolean
  sidebarExpanded: boolean
  menuHoverActive: boolean
  activeMenuItem: string | null
  activePath: string | null
  mobileMenuActive: boolean
  anchored: boolean
}

const layoutConfig = reactive<LayoutConfig>({
  preset: 'Aura',
  primary: 'emerald',
  surface: null,
  darkTheme: false,
  menuMode: 'static'
})

const layoutState = reactive<LayoutState>({
  staticMenuInactive: false,
  overlayMenuActive: false,
  profileSidebarVisible: false,
  configSidebarVisible: false,
  sidebarExpanded: false,
  menuHoverActive: false,
  activeMenuItem: null,
  activePath: null,
  mobileMenuActive: false,
  anchored: false
})

export function useLayout() {
  const toggleDarkMode = () => {
    if (!document.startViewTransition) {
      executeDarkModeToggle()
      return
    }

    document.startViewTransition(() => executeDarkModeToggle())
  }

  const executeDarkModeToggle = () => {
    layoutConfig.darkTheme = !layoutConfig.darkTheme
    document.documentElement.classList.toggle('app-dark')
  }

  const toggleMenu = () => {
    if (isDesktop()) {
      if (layoutConfig.menuMode === 'static') {
        layoutState.staticMenuInactive = !layoutState.staticMenuInactive
      }

      if (layoutConfig.menuMode === 'overlay') {
        layoutState.overlayMenuActive = !layoutState.overlayMenuActive
      }
    } else {
      layoutState.mobileMenuActive = !layoutState.mobileMenuActive
    }
  }

  const toggleConfigSidebar = () => {
    layoutState.configSidebarVisible = !layoutState.configSidebarVisible
  }

  const hideMobileMenu = () => {
    layoutState.mobileMenuActive = false
  }

  const changeMenuMode = (event: { value: 'static' | 'overlay' }) => {
    layoutConfig.menuMode = event.value
    layoutState.staticMenuInactive = false
    layoutState.mobileMenuActive = false
    layoutState.sidebarExpanded = false
    layoutState.menuHoverActive = false
    layoutState.anchored = false
  }

  const isDarkTheme = computed(() => layoutConfig.darkTheme)
  const isDesktop = () => window.innerWidth > DESKTOP_BREAKPOINT

  const hasOpenOverlay = computed(() => layoutState.overlayMenuActive)

  return {
    layoutConfig,
    layoutState,
    isDarkTheme,
    toggleDarkMode,
    toggleConfigSidebar,
    toggleMenu,
    hideMobileMenu,
    changeMenuMode,
    isDesktop,
    hasOpenOverlay
  }
}
