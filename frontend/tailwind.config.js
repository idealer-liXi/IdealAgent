export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        surface: '#f8f9fa',
        elevated: '#ffffff',
        dark: '#1a1a2e',
        'text-primary': '#1a1a2e',
        'text-secondary': '#6b7280',
        'text-tertiary': '#9ca3af',
        accent: '#2563eb',
        'accent-hover': '#1d4ed8',
        'accent-light': '#eff6ff',
        'border-default': '#e5e7eb',
        'border-subtle': '#f1f3f5',
        error: '#ef4444',
        'error-bg': '#fef2f2',
        success: '#10b981',
        'success-bg': '#ecfdf5',
      },
      borderRadius: {
        'card-sm': '8px',
        'card-md': '12px',
        'card-lg': '16px',
        'card-xl': '20px',
      },
      boxShadow: {
        'card': '0 1px 2px rgba(0, 0, 0, 0.04)',
        'card-hover': '0 4px 12px rgba(0, 0, 0, 0.05)',
        'card-lg': '0 8px 24px rgba(0, 0, 0, 0.06)',
        'sidebar': '4px 0 16px rgba(0, 0, 0, 0.04)',
      },
      transitionTimingFunction: {
        'smooth': 'cubic-bezier(0.4, 0, 0.2, 1)',
      },
    }
  },
  plugins: []
}
