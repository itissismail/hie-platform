package com.hie.platform.gateway.service;

/*import com.hie.platform.shared.dto.HL7Message;
import com.hie.platform.shared.model.MessageState;
import com.hie.platform.shared.model.MessageStatus;
import com.hie.platform.shared.repository.MessageStateRepository;
import com.hie.platform.shared.service.AuditService;
import com.hie.platform.shared.service.MessagePublisher;*/
import org.springframework.stereotype.Service;

@Service
@Deprecated
public class MessageIngestionService {

    /*private static final Logger logger = LoggerFactory.getLogger(MessageIngestionService.class);

    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private AuditService auditService;

    @Autowired
    private MessageStateRepository messageStateRepository;

    @Autowired
    private HL7ParserService hl7ParserService;

    public String ingestMessage(HL7Message message) {
        long startTime = System.currentTimeMillis();

        try {
            // Parse HL7 message to extract metadata
            hl7ParserService.parseMessage(message);

            // Create message state record
            MessageState state = new MessageState();
            state.setMessageId(message.getMessageId());
            state.setCurrentStatus(MessageStatus.RECEIVED);
            state.setSourceOrganization(message.getSourceOrganization());
            state.setMessageType(message.getMessageType());
            state.setPatientId(message.getPatientId());

            messageStateRepository.save(state);

            // Publish to validation queue
            messagePublisher.publishToValidationQueue(message);

            // Log success
            long processingTime = System.currentTimeMillis() - startTime;
            auditService.logSuccess(message.getMessageId(), message.getCorrelationId(),
                    "API_GATEWAY", processingTime);

            logger.info("Message ingested successfully: {}", message.getCorrelationId());

            return message.getCorrelationId().toString();

        } catch (Exception e) {
            auditService.logFailure(message.getMessageId(), message.getCorrelationId(),
                    "API_GATEWAY", e.getMessage());
            logger.error("Error ingesting message", e);
            throw new RuntimeException("Failed to ingest message", e);
        }
    }

    public String getMessageStatus(UUID correlationId) {
        // Implementation to get message status
        return "PROCESSING"; // Placeholder
    }*/
}