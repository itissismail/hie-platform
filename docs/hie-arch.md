flowchart LR

    subgraph External[External System]
        extMsg[HL7 v2 Messages ADT ORU MDM]
    end

    subgraph Gateway[API Gateway Service]
        jwt[JWT Validation & Role Check]
    end

    subgraph Router[Message Router Service]
        store[Upload Raw HL7 to Object Storage S3 MinIO]
        audit[Audit Entry to Database]
        vq[Publish to Validation Queue RabbitMQ]
    end

    subgraph Validation[Validation Service]
        quarantine[Quarantine Rules & Validation]
        iq[If Valid to Intake Queue]
    end

    subgraph Intake[Intake Service]
        mpi[MPI Logic Patient Linking]
        cq[Publish to Conversion Queue]
    end

    subgraph Conversion[Conversion Service]
        fhir[FHIR Conversion HL7 v2 to FHIR]
        sq[Publish to Storage Queue]
    end

    subgraph Storage[Storage Service]
        persist[Business Logic and Save to Database]
    end

    %% Connections
    extMsg --> Gateway --> Router
    Router --> Validation
    Validation --> Intake
    Intake --> Conversion
    Conversion --> Storage

    %% Detailed flows inside Router
    Gateway --> jwt --> Router
    Router --> store
    Router --> audit
    Router --> vq

    %% Validation flow
    vq --> quarantine --> iq
    iq --> Intake

    %% Intake flow
    Intake --> mpi --> cq
    cq --> Conversion

    %% Conversion flow
    Conversion --> fhir --> sq
    sq --> Storage

    %% Storage flow
    Storage --> persist

