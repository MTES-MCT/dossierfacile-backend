INSERT INTO public.apartment_sharing (id, token, token_public, operator_date, application_type, dossier_pdf_document_status, last_update_date, pdf_dossier_file_id)
VALUES
(
    1, -- id
    NULL, -- token
    NULL, -- token_public
    CURRENT_TIMESTAMP, -- operator_date
    'COUPLE', -- application_type
    NULL,
    CURRENT_TIMESTAMP, -- last_update_date
    NULL -- pdf_dossier_file_id
);

INSERT INTO public.user_account (id, creation_date, email, first_name, last_name,
    last_login_date, update_date_time, enabled, keycloak_id, france_connect,
    preferred_name, user_type)
VALUES
(
    1,
    CURRENT_TIMESTAMP, -- creation_date
    'test@example.com', -- email
    'Prénom', -- first_name
    'Nom', -- last_name
    CURRENT_TIMESTAMP, -- last_login_date
    CURRENT_TIMESTAMP, -- update_date_time
    true, -- enabled
    'keycloak_id', -- keycloak_id
    false, -- france_connect
    'Nom préféré', -- preferred_name
    'TENANT' -- user_type
);

INSERT INTO public.tenant (id, tenant_type, apartment_sharing_id,
    honor_declaration, last_update_date, status, warnings)
VALUES
(
    1, -- id
    'CREATE', -- tenant_type
    1, -- apartment_sharing_id
    true, -- honor_declaration
    CURRENT_TIMESTAMP, -- last_update_date
    'ARCHIVED', -- status
    0
);


INSERT INTO public.user_account (id, creation_date, email, first_name, last_name,
    last_login_date, update_date_time, enabled, keycloak_id, france_connect,
    preferred_name, user_type)
VALUES
(
    2,
    CURRENT_TIMESTAMP, -- creation_date
    NULL, -- email
    'PrenomConjoint', -- first_name
    'NomConjoint', -- last_name
    CURRENT_TIMESTAMP, -- last_login_date
    CURRENT_TIMESTAMP, -- update_date_time
    true, -- enabled
    'keycloak_id', -- keycloak_id
    false, -- france_connect
    'Toto', -- preferred_name
    'TENANT' -- user_type
);

INSERT INTO public.tenant (id, tenant_type, apartment_sharing_id,
    honor_declaration, last_update_date, status, warnings)
VALUES
(
    2, -- id
    'JOIN', -- tenant_type
    1, -- apartment_sharing_id
    true, -- honor_declaration
    CURRENT_TIMESTAMP, -- last_update_date
    'INCOMPLETE', -- status
    0
);
