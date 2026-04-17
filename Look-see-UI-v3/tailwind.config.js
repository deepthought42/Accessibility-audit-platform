/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: ['class', '[data-theme="dark"]'],
  content: [
    "./src/**/*.{html,ts}",
    "./node_modules/flowbite/**/*.js"
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#fef2f5',
          100: '#fee2e8',
          200: '#ffc9d6',
          300: '#ff9db5',
          400: '#ff6690',
          500: '#FF0050',
          600: '#e6004a',
          700: '#c2003e',
          800: '#a10039',
          900: '#870035',
          950: '#4d0018',
        },
        // `ink` is the canonical neutral palette. `charcoal` is kept as an
        // alias for legacy templates; new code should use `ink`.
        ink: {
          50: '#f6f5f5',
          100: '#e7e6e6',
          200: '#d1d0d0',
          300: '#b1afb0',
          400: '#8a8889',
          500: '#6f6d6e',
          600: '#5f5d5e',
          700: '#504e4f',
          800: '#464445',
          900: '#3d3b3c',
          950: '#231f20',
        },
        charcoal: {
          50: '#f6f5f5',
          100: '#e7e6e6',
          200: '#d1d0d0',
          300: '#b1afb0',
          400: '#8a8889',
          500: '#6f6d6e',
          600: '#5f5d5e',
          700: '#504e4f',
          800: '#464445',
          900: '#3d3b3c',
          950: '#231f20',
        },
        slate: {
          850: '#1a2332',
          925: '#111827',
        },
        score: {
          good: '#10b981',
          'good-bg': '#d1fae5',
          'good-text': '#065f46',
          warning: '#f59e0b',
          'warning-bg': '#fef3c7',
          'warning-text': '#92400e',
          critical: '#ef4444',
          'critical-bg': '#fee2e2',
          'critical-text': '#991b1b',
        },
        // Semantic aliases reading from CSS custom properties so components
        // automatically theme (light/dark) when the variable flips.
        surface: {
          base: 'var(--surface-base)',
          raised: 'var(--surface-raised)',
          sunken: 'var(--surface-sunken)',
          inverse: 'var(--surface-inverse)',
        },
      },
      fontFamily: {
        display: ['Cera Pro', 'Inter', 'system-ui', 'sans-serif'],
        body: ['Inter', 'Open Sans', 'system-ui', 'sans-serif'],
      },
      fontSize: {
        '2xs': ['0.625rem', { lineHeight: '0.875rem' }],
      },
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
        '128': '32rem',
      },
      borderRadius: {
        'xs': '2px',
        'sm': '4px',
        'md': '6px',
        'lg': '8px',
        'xl': '12px',
        '2xl': '16px',
        '3xl': '1.5rem',
      },
      boxShadow: {
        // Named tokens matching tokens.scss
        'token-xs': 'var(--shadow-xs)',
        'token-sm': 'var(--shadow-sm)',
        'token-md': 'var(--shadow-md)',
        'token-lg': 'var(--shadow-lg)',
        'token-xl': 'var(--shadow-xl)',
        'focus':    'var(--focus-ring)',
        // Legacy aliases (kept so existing templates keep working)
        'card':       '0 1px 3px 0 rgb(0 0 0 / 0.06), 0 1px 2px -1px rgb(0 0 0 / 0.06)',
        'card-hover': '0 4px 6px -1px rgb(0 0 0 / 0.08), 0 2px 4px -2px rgb(0 0 0 / 0.05)',
        'panel':      '0 1px 2px 0 rgb(0 0 0 / 0.05)',
        'elevated':   '0 10px 15px -3px rgb(0 0 0 / 0.08), 0 4px 6px -4px rgb(0 0 0 / 0.04)',
      },
      transitionDuration: {
        fast: '120ms',
        base: '180ms',
        slow: '280ms',
      },
      transitionTimingFunction: {
        'out-soft':    'cubic-bezier(0.22, 1, 0.36, 1)',
        'in-out-soft': 'cubic-bezier(0.65, 0, 0.35, 1)',
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.3s ease-out',
        'slide-in-left': 'slideInLeft 0.2s ease-out',
        'pulse-subtle': 'pulseSubtle 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideInLeft: {
          '0%': { opacity: '0', transform: 'translateX(-10px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
        pulseSubtle: {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0.7' },
        },
      },
    },
  },
  plugins: [
    require('flowbite/plugin')
  ],
}
