<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import LabProviderList from './LabProviderList.vue'
import LabTestList from './LabTestList.vue'
import LabProviderTestPanel from './LabProviderTestPanel.vue'
import LabPanelList from './LabPanelList.vue'

const { t } = useI18n()
const activeTab = ref('providers')
</script>

<template>
  <div class="lab-catalog-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('lab.catalog.title') }}</h1>
    </div>

    <Tabs v-model:value="activeTab">
      <TabList>
        <Tab value="providers">{{ t('lab.catalog.tabs.providers') }}</Tab>
        <Tab value="tests">{{ t('lab.catalog.tabs.tests') }}</Tab>
        <Tab value="providerTests">{{ t('lab.catalog.tabs.providerTests') }}</Tab>
        <Tab value="panels">{{ t('lab.catalog.tabs.panels') }}</Tab>
      </TabList>
      <TabPanels>
        <TabPanel value="providers"><LabProviderList /></TabPanel>
        <TabPanel value="tests"><LabTestList /></TabPanel>
        <TabPanel value="providerTests"><LabProviderTestPanel /></TabPanel>
        <TabPanel value="panels"><LabPanelList /></TabPanel>
      </TabPanels>
    </Tabs>
  </div>
</template>

<style scoped>
.lab-catalog-page {
  max-width: 1000px;
  margin: 0 auto;
}
.page-header {
  margin-bottom: 1.5rem;
}
.page-title {
  margin: 0;
}
</style>
