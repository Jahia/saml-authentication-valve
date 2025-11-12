import {defineConfig} from 'cypress';

export default defineConfig({
    video: true,
    chromeWebSecurity: false,
    defaultCommandTimeout: 40000,
    pageLoadTimeout: 90000,
    requestTimeout: 15000,
    responseTimeout: 40000,
    reporter: 'cypress-multi-reporters',
    reporterOptions: {
        configFile: 'reporter-config.json'
    },
    screenshotsFolder: './results/screenshots',
    videosFolder: './results/videos',
    viewportWidth: 1366,
    viewportHeight: 768,
    retries: {
        // Ajouter des retries pour les tests qui échouent en CI
        runMode: 2, // 2 retries en mode CI
        openMode: 0 // Pas de retry en mode dev
    },
    e2e: {
        // We've imported your old cypress plugins here.
        // You may want to clean this up later by importing these.
        setupNodeEvents(on, config) {
            // eslint-disable-next-line @typescript-eslint/no-var-requires
            return require('./cypress/plugins/index.js')(on, config);
        },
        excludeSpecPattern: ['**/*.ignore.ts']
    }
});
