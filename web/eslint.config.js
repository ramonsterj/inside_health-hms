import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import pluginVue from 'eslint-plugin-vue'
import pluginVueI18n from '@intlify/eslint-plugin-vue-i18n'
import pluginSecurity from 'eslint-plugin-security'
import eslintConfigPrettier from 'eslint-config-prettier'
import globals from 'globals'

export default tseslint.config(
  {
    ignores: ['dist/**', 'node_modules/**', '*.config.js', '*.config.ts']
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  ...pluginVueI18n.configs['flat/base'],
  pluginSecurity.configs.recommended,
  {
    languageOptions: {
      globals: {
        ...globals.browser
      }
    },
    settings: {
      'vue-i18n': {
        localeDir: './src/i18n/locales/*.json',
        messageSyntaxVersion: '^10.0.0'
      }
    }
  },
  {
    // i18n guards: no new hardcoded UI text, and every t() key must exist.
    // The locale parity test (src/i18n/locales.parity.spec.ts) is what keeps
    // the two locale files key-for-key in sync; these rules stop drift at the
    // source. no-unused-keys is intentionally omitted — this codebase builds
    // many keys dynamically (e.g. t('common.time.' + unit)), which the static
    // rule cannot resolve and would false-positive on.
    files: ['**/*.vue'],
    rules: {
      '@intlify/vue-i18n/no-missing-keys': 'error',
      '@intlify/vue-i18n/no-raw-text': [
        'error',
        {
          // Also scan common label-ish props on any element (PrimeVue & native).
          attributes: {
            '/.+/': [
              'label',
              'placeholder',
              'title',
              'aria-label',
              'header',
              'choose-label',
              'chooseLabel',
              'upload-label',
              'cancel-label',
              'empty-message',
              'emptyMessage'
            ]
          },
          // Strings made up solely of whitespace, digits, punctuation or symbols
          // are not translatable prose (e.g. "*", "/", ":", "mmHg" units live in
          // the locale files already).
          ignorePattern: '^[\\s\\d\\p{P}\\p{S}]+$',
          ignoreText: [
            '*',
            '/',
            '-',
            ':',
            '|',
            '·',
            '×',
            '%',
            '#',
            '@',
            '&',
            // "x" multiplier in "{qty} x {price}" rows.
            'x',
            // SOAP note section mnemonics — identical in Spanish (Subjetivo,
            // Objetivo, Análisis, Plan), so not translatable prose.
            'S',
            'O',
            'A',
            'P'
          ]
        }
      ]
    }
  },
  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser
      }
    }
  },
  {
    rules: {
      // TypeScript rules
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/explicit-function-return-type': 'off',
      '@typescript-eslint/explicit-module-boundary-types': 'off',

      // Vue rules
      'vue/multi-word-component-names': 'off',
      'vue/no-v-html': 'warn',
      'vue/require-default-prop': 'off',
      'vue/component-name-in-template-casing': ['error', 'PascalCase'],
      'vue/attribute-hyphenation': 'off',
      'vue/attributes-order': 'off',

      // General rules
      'no-console': 'warn',
      'no-debugger': 'error',

      // Date / time formatting must go through @/utils/format. See CLAUDE.md
      // § "Date / Time Formatting" and ARCHITECTURE.md § "Date and Time Handling".
      // The vue/no-restricted-syntax rule below mirrors these for <template> expressions.
      'no-restricted-syntax': [
        'error',
        {
          selector: "CallExpression[callee.property.name='toLocaleDateString']",
          message:
            "Use formatDate() from '@/utils/format' instead of toLocaleDateString — output must be locale-independent dd/MM/yyyy."
        },
        {
          selector: "CallExpression[callee.property.name='toLocaleTimeString']",
          message:
            "Use formatTime() from '@/utils/format' instead of toLocaleTimeString — output must be locale-independent HH:mm."
        },
        {
          selector: "CallExpression[callee.property.name='toLocaleString']",
          message:
            "Use formatDateTime() from '@/utils/format' instead of toLocaleString — output must be locale-independent dd/MM/yyyy - HH:mm."
        },
        {
          selector: "CallExpression[callee.name='d'][arguments.length>=1]",
          message:
            "Use formatDate / formatTime / formatDateTime from '@/utils/format' instead of vue-i18n's d() — there is no datetimeFormats config and d() falls back to browser locale."
        },
        {
          selector:
            "CallExpression[callee.object.callee.property.name='toISOString'][callee.property.name='substring']",
          message:
            "Use toApiDate() from '@/utils/format' instead of toISOString().substring(0, 10) — the inline form has a UTC-shift bug in Guatemala (UTC-6)."
        },
        {
          selector:
            "CallExpression[callee.object.callee.property.name='toISOString'][callee.property.name='split']",
          message:
            "Use toApiDate() from '@/utils/format' instead of toISOString().split('T') — the inline form has a UTC-shift bug in Guatemala (UTC-6)."
        },
        // Documents must never open in a new tab/window — the customer's
        // Chromebooks can't afford the extra renderer process. Use the in-app
        // FileViewerDialog instead. See docs/features/in-app-document-viewer.md.
        {
          selector: "CallExpression[callee.object.name='window'][callee.property.name='open']",
          message:
            'Do not open documents in a new tab — render them in-app with FileViewerDialog (src/components/viewer). See docs/features/in-app-document-viewer.md.'
        }
      ],

      // Same rules, applied to expressions inside <template>.
      'vue/no-restricted-syntax': [
        'error',
        {
          selector: "CallExpression[callee.property.name='toLocaleDateString']",
          message:
            "Use formatDate() from '@/utils/format' instead of toLocaleDateString — output must be locale-independent dd/MM/yyyy."
        },
        {
          selector: "CallExpression[callee.property.name='toLocaleTimeString']",
          message:
            "Use formatTime() from '@/utils/format' instead of toLocaleTimeString — output must be locale-independent HH:mm."
        },
        {
          selector: "CallExpression[callee.property.name='toLocaleString']",
          message:
            "Use formatDateTime() from '@/utils/format' instead of toLocaleString — output must be locale-independent dd/MM/yyyy - HH:mm."
        },
        {
          selector: "CallExpression[callee.name='d'][arguments.length>=1]",
          message:
            "Use formatDate / formatTime / formatDateTime from '@/utils/format' instead of vue-i18n's d() — there is no datetimeFormats config and d() falls back to browser locale."
        },
        // Search fields must use the shared <SearchInput> so the platform-wide
        // as-you-type behavior (debounced, fires after 3 characters) is guaranteed
        // and any new search field inherits it. Hand-rolled IconField search boxes
        // (an <InputIcon class="pi pi-search"> next to an <InputText>) are banned.
        // See docs/architecture/SEARCH.md.
        {
          selector:
            "VElement[name='inputicon'] VAttribute[key.name='class'] > VLiteral[value=/pi-search/]",
          message:
            'Build search fields with <SearchInput> from @/components/common — not a raw IconField + InputText. SearchInput provides the debounced 3-character autocomplete every search field must have. See docs/architecture/SEARCH.md.'
        },
        {
          selector: "CallExpression[callee.object.name='window'][callee.property.name='open']",
          message:
            'Do not open documents in a new tab — render them in-app with FileViewerDialog (src/components/viewer). See docs/features/in-app-document-viewer.md.'
        }
      ]
    }
  },
  {
    // The shared formatter library is allowed to call the underlying APIs.
    files: ['src/utils/format.ts'],
    rules: {
      'no-restricted-syntax': 'off',
      'vue/no-restricted-syntax': 'off'
    }
  },
  {
    // SearchInput is the sanctioned implementation of the search box, so it is
    // the one place allowed to use the IconField search-icon pattern directly.
    files: ['src/components/common/SearchInput.vue'],
    rules: {
      'vue/no-restricted-syntax': 'off'
    }
  },
  eslintConfigPrettier
)
