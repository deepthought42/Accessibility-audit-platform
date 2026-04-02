import { MessageService } from './message.service';

describe('MessageService', () => {
  let service: MessageService;

  beforeEach(() => {
    service = new MessageService();
  });

  it('emits info messages by default', (done) => {
    service.messages$.subscribe((message) => {
      expect(message).toEqual({ content: 'hello', style: 'info' });
      done();
    });

    service.add('hello');
  });

  it('clears the current message', (done) => {
    service.messages$.subscribe((message) => {
      expect(message).toEqual({ content: '' });
      done();
    });

    service.clear();
  });
});
