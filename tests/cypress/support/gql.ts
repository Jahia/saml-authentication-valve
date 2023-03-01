/* eslint-disable @typescript-eslint/no-explicit-any */
import Chainable = Cypress.Chainable

interface AuthMethod {
    token?: string
    username?: string
    password?: string
}

export const createTask = (taskService: string, taskName: string, auth?: AuthMethod): Chainable<any> => {
    if (auth) {
        cy.apolloClient(auth);
    }

    return cy.apollo({
        mutationFile: 'createTask.graphql',
        variables: {
            service: taskService,
            name: taskName
        },
        errorPolicy: 'all'
    });
};

export const deleteTask = (taskService: string, taskName: string, auth?: AuthMethod): Chainable<any> => {
    if (auth) {
        cy.apolloClient(auth);
    }

    return cy.apollo({
        mutationFile: 'deleteTask.graphql',
        variables: {
            service: taskService,
            name: taskName
        },
        errorPolicy: 'all'
    });
};

export const healthCheck = (severity: string, auth?: AuthMethod): Chainable<any> => {
    if (auth) {
        cy.apolloClient(auth);
    }

    return cy
        .apollo({
            queryFile: 'healthcheck.graphql',
            variables: {
                severity
            },
            errorPolicy: 'all'
        })
        .then((response: any) => {
            return response.data.admin.jahia.healthCheck;
        });
};

export const load = (interval: string, auth?: AuthMethod): Chainable<any> => {
    if (auth) {
        cy.apolloClient(auth);
    }

    return cy
        .apollo({
            queryFile: 'load.graphql',
            variables: {
                interval
            },
            errorPolicy: 'all'
        })
        .then((response: any) => {
            console.log(response);
            return response.data.admin.jahia.load;
        });
};
