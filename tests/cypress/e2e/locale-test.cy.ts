describe('Locale Test Demonstration', () => {
    it('should verify default browser locale', () => {
        cy.visit('/');

        // Check current browser locale
        cy.getBrowserLocale().then((locale) => {
            cy.log(`Default browser locale: ${locale}`);
        });

        // Test default locale formatting
        cy.testLocaleFormatting('default');
    });

    it('should change to French locale and verify', () => {
        cy.visit('/');

        // Set French locale directly in test
        cy.setLocale('fr-FR');
        cy.setLanguageHeaders('fr-FR');

        // Reload to apply locale changes
        cy.reload();

        // Verify the locale change
        cy.getBrowserLocale().then((locale) => {
            cy.log(`Browser locale after change: ${locale}`);
        });

        // Test French locale formatting
        cy.testLocaleFormatting('fr-FR');

        // Verify date formatting shows French format
        cy.window().then((win) => {
            const testDate = new Date('2023-12-25');
            const formattedDate = testDate.toLocaleDateString();
            cy.log(`French date format: ${formattedDate}`);
            // French format should be 25/12/2023
            expect(formattedDate).to.match(/25[\/\.]12[\/\.]2023/);
        });
    });

    it('should change to German locale and verify', () => {
        cy.visit('/');

        // Set German locale directly in test
        cy.setLocale('de-DE');
        cy.setLanguageHeaders('de-DE');

        // Reload to apply locale changes
        cy.reload();

        // Verify the locale change
        cy.getBrowserLocale().then((locale) => {
            cy.log(`Browser locale after change: ${locale}`);
        });

        // Test German locale formatting
        cy.testLocaleFormatting('de-DE');

        // Verify date formatting shows German format
        cy.window().then((win) => {
            const testDate = new Date('2023-12-25');
            const formattedDate = testDate.toLocaleDateString();
            cy.log(`German date format: ${formattedDate}`);
            // German format should be 25.12.2023
            expect(formattedDate).to.match(/25[\/\.]12[\/\.]2023/);
        });
    });

    it('should switch between multiple locales in same test', () => {
        cy.visit('/');

        // Test English first
        cy.setLocale('en-US');
        cy.setLanguageHeaders('en-US');
        cy.reload();
        cy.testLocaleFormatting('en-US');

        // Switch to French
        cy.setLocale('fr-FR');
        cy.setLanguageHeaders('fr-FR');
        cy.reload();
        cy.testLocaleFormatting('fr-FR');

        // Switch to Spanish
        cy.setLocale('es-ES');
        cy.setLanguageHeaders('es-ES');
        cy.reload();
        cy.testLocaleFormatting('es-ES');
    });
});
