/** @type {import('tailwindcss').Config} */
module.exports = {
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
          warning: '#f59e0b',
          critical: '#ef4444',
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
        'xl': '0.75rem',
        '2xl': '1rem',
        '3xl': '1.5rem',
      },
      boxShadow: {
        'card': '0 1px 3px 0 rgb(0 0 0 / 0.06), 0 1px 2px -1px rgb(0 0 0 / 0.06)',
        'card-hover': '0 4px 6px -1px rgb(0 0 0 / 0.08), 0 2px 4px -2px rgb(0 0 0 / 0.05)',
        'panel': '0 1px 2px 0 rgb(0 0 0 / 0.05)',
        'elevated': '0 10px 15px -3px rgb(0 0 0 / 0.08), 0 4px 6px -4px rgb(0 0 0 / 0.04)',
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
