import {defineConfig} from 'cypress';

export default defineConfig({
    chromeWebSecurity: false,
    defaultCommandTimeout: 30000,
    reporter: 'cypress-multi-reporters',
    reporterOptions: {
        configFile: 'reporter-config.json'
    },
    screenshotsFolder: './results/screenshots',
    videosFolder: './results/videos',
    viewportWidth: 1366,
    viewportHeight: 768,
    e2e: {
        // We've imported your old cypress plugins here.
        // You may want to clean this up later by importing tcleahese.
        setupNodeEvents(on, config) {
            on('before:browser:launch', (browser, launchOptions) => {
                // `args` is an array of all the arguments that will
                // be passed to browsers when it launches

                if (browser.family === 'chromium' && browser.name !== 'electron') {
                    // Auto open devtools
                    launchOptions.args.push('--auto-open-devtools-for-tabs');
                    // Specify accept language french
                    launchOptions.args.push('--accept-lang=fr');
                }

                if (browser.family === 'firefox') {
                    // Auto open devtools
                    launchOptions.args.push('-devtools');
                    launchOptions.preferences['intl.accept_languages'] = 'fr-ca,fr,en-ca,en-us,en';
                }

                if (browser.name === 'electron') {
                    // Auto open devtools
                    launchOptions.preferences.devTools = false;
                }

                console.log(launchOptions.args); // Print all current args
                // Whatever you return here becomes the launchOptions
                return launchOptions;
            });
            // eslint-disable-next-line @typescript-eslint/no-var-requires
            return require('./cypress/plugins/index.js')(on, config);
        },
        excludeSpecPattern: ['*.ignore.ts', '**/*.en.cy.ts']
    }
});
