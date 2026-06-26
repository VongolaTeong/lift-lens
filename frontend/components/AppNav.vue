<script setup lang="ts">
const route = useRoute()

const links = [
  { to: '/', label: 'Dashboard', exact: true },
  { to: '/exercises', label: 'Exercises' },
  { to: '/muscles', label: 'Muscle Balance' },
  { to: '/insights', label: 'Insights' },
  { to: '/import', label: 'Import' },
]

function isActive(link: { to: string; exact?: boolean }) {
  return link.exact ? route.path === link.to : route.path.startsWith(link.to)
}
</script>

<template>
  <header class="nav">
    <div class="nav-inner">
      <NuxtLink to="/" class="brand">
        <span class="brand-mark">◧</span>
        <span class="brand-name">Lift<span>Lens</span></span>
      </NuxtLink>
      <nav class="nav-links">
        <NuxtLink
          v-for="l in links"
          :key="l.to"
          :to="l.to"
          class="nav-link"
          :class="{ active: isActive(l) }"
        >
          {{ l.label }}
        </NuxtLink>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.nav {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(11, 14, 20, 0.85);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
}

.nav-inner {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 9px;
  font-weight: 700;
  font-size: 18px;
}

.brand-mark {
  color: var(--accent);
  font-size: 20px;
}

.brand-name span {
  color: var(--accent);
}

.nav-links {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.nav-link {
  padding: 7px 13px;
  border-radius: var(--radius-sm);
  color: var(--text-muted);
  font-weight: 550;
  font-size: 14px;
  transition: background 0.15s ease, color 0.15s ease;
}

.nav-link:hover {
  background: var(--surface-2);
  color: var(--text);
}

.nav-link.active {
  background: var(--accent-soft);
  color: var(--accent);
}

@media (max-width: 620px) {
  .nav-inner {
    height: auto;
    flex-direction: column;
    align-items: flex-start;
    padding: 12px 16px;
    gap: 10px;
  }
}
</style>
