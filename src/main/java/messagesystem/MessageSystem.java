package messagesystem;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MessageSystem {
    private final Map<Address, ConcurrentLinkedQueue<Message>> messages = new HashMap<>();
    private final AddressService addressService = new AddressService();

    public AddressService getAddressService() {
        return addressService;
    }

    public void addService(Abonent abonent) {
        messages.put(abonent.getAddress(), new ConcurrentLinkedQueue<>());
    }

    public void sendMessage(Message message) {
        if(message.getTo() != null) {
            messages.get(message.getTo()).add(message);
        }
    }

    public void execForAbonent(Abonent abonent) {
        final ConcurrentLinkedQueue<Message> queue = messages.get(abonent.getAddress());
        while (!queue.isEmpty()) {
            final Message message = queue.poll();
            message.exec(abonent);
        }
    }
}
