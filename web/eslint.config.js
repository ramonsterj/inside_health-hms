import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import pluginVue from 'eslint-plugin-vue'
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
  pluginSecurity.configs.recommended,
  {
    languageOptions: {
      globals: {
        ...globals.browser
      }
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
  eslintConfigPrettier
)
