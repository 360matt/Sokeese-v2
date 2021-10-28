package fr.i360matt.sokeese.server;

import fr.i360matt.sokeese.common.redistribute.Packet;
import fr.i360matt.sokeese.common.redistribute.SendPacket;
import fr.i360matt.sokeese.common.redistribute.reply.Reply;
import fr.i360matt.sokeese.common.redistribute.reply.SendReply;

import java.util.Arrays;

public class Router {

    private final SokeeseServer server;

    public Router (final SokeeseServer server) {
        this.server = server;
    }


    public void incomingRequest (final Object obj, final LoggedClient user) {
        if (obj instanceof SendPacket) {
            this.asPacket((SendPacket) obj, user);
            return;
        }

        if (obj instanceof SendReply) {
            this.asReply((SendReply) obj, user);
            return;
        }
        this.server.getCatcherServer().callWithRequest(obj, CatcherServer.EMPTY___REQUEST_DATA);
    }



    private void asReply (final SendReply sendReply, final LoggedClient client) {
        if (sendReply.getObj() instanceof SendPacket || sendReply.getObj() instanceof Packet) {
            return;
            // fixed exploit/bug: infinite recursive.
        }

        if (sendReply.getRecipient() instanceof String) {
            final LoggedClient candidate = server.getClient((String) sendReply.getRecipient());
            if (candidate != null) {
                final Reply reply = new Reply(
                        client.getClientName(),
                        sendReply.getObj(),
                        sendReply.getId()
                );
                candidate.send(reply);
            }
        } else {
            this.server.getCatcherServer().replyForServer(sendReply, client);
        }
    }

    private void asPacket (final SendPacket sendPacket, final LoggedClient user) {
        if (sendPacket.getObj() instanceof SendPacket || sendPacket.getObj() instanceof Packet) {
            return;
            // fixed exploit/bug: infinite recursive.
        }



        if (sendPacket.getRecipient() instanceof String) {
            final String recipient = (String) sendPacket.getRecipient();
            if (recipient.equals("")) {
                // empty recipient = the recipient is server
                this.server.getCatcherServer().packetForServer(sendPacket, user);
                return;
            }

            if (recipient.equals("*")) {
                final Packet packet = new Packet(
                        sendPacket.getObj(),
                        user.getClientName(),
                        sendPacket.getId()
                );

                for (final LoggedClient client : this.server.getClientsManager().getInstances())
                    client.send(packet);
            } else if (recipient.equals("**")) {
                final Packet packet = new Packet(
                        sendPacket.getObj(),
                        user.getClientName(),
                        sendPacket.getId()
                );

                for (final LoggedClient client : this.server.getClientsManager().getInstances())
                    client.send(packet);

                this.server.getCatcherServer().packetForServer(sendPacket, user);
            } else {
                final LoggedClient candidate = this.server.getClient(recipient);
                if (candidate != null) {
                    final Packet packet = new Packet(
                            sendPacket.getObj(),
                            user.getClientName(),
                            sendPacket.getId()
                    );

                    candidate.send(packet);
                }
            }
        } else if (sendPacket.getRecipient() instanceof String[]) {
            final String[] fixedRecipient = (String[]) Arrays.stream((String[]) sendPacket.getRecipient()).distinct().toArray();
            // fixed exploit/bug: amplified DDoS with multiple same client name.

            Packet packet = null;

            for (final String candidateName : fixedRecipient) {
                if (candidateName == null) {
                    this.server.getCatcherServer().packetForServer(sendPacket, user);
                    continue;
                }

                final LoggedClient candidate = server.getClient(candidateName);
                if (candidate != null) {
                    if (packet == null) {
                        packet = new Packet(
                                sendPacket.getObj(),
                                user.getClientName(),
                                sendPacket.getId()
                        );
                    }
                    candidate.send(packet);
                }
            }
        } else {
            // empty recipient = the recipient is server
            this.server.getCatcherServer().packetForServer(sendPacket, user);
        }

    }

}
