import {NodeSSH} from 'node-ssh';

interface Connection {
    hostname: string
    port: string
    username: string
    password: string
}

const sshCommand = async (commands: Array<string>, connection: Connection) => {
    const ssh = new NodeSSH();
    console.log(connection);
    console.log(`SSH connection to: ${connection.hostname}`);
    await ssh.connect({
        host: connection.hostname,
        port: connection.port,
        username: connection.username,
        password: connection.password
    });

    const response = await ssh.exec(commands.join(';'), []);
    ssh.dispose();

    return response;
};

module.exports = sshCommand;
