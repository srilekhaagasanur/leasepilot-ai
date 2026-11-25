-- V1: the full LeasePilot schema as of Nov 2026 (users, properties, units, tenants, leases,
-- maintenance requests, AI drafts, work orders, password reset links).
--
-- Flyway runs this only on an empty database. A database that already has these tables
-- (created earlier by Hibernate's ddl-auto) is marked as "already at version 1" instead
-- (spring.flyway.baseline-on-migrate), so this file is skipped there.
--
-- Never edit a migration that has already run somewhere. Add a new V<n>__description.sql instead.

create table agent_drafts (
    cited_clause_id bigint,
    created_at datetime(6) not null,
    decided_at datetime(6),
    id bigint not null auto_increment,
    request_id bigint not null,
    vendor_id bigint,
    approval_status varchar(30) not null,
    responsibility varchar(30) not null,
    manager_note TEXT,
    reasoning TEXT,
    tenant_reply_text TEXT not null,
    tool_trace TEXT,
    work_order_text TEXT not null,
    primary key (id)
) engine=InnoDB;

create table app_users (
    demo bit not null,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    role varchar(30) not null,
    email varchar(255) not null,
    full_name varchar(255) not null,
    password_hash varchar(255),
    primary key (id)
) engine=InnoDB;

create table lease_clauses (
    id bigint not null auto_increment,
    lease_id bigint not null,
    clause_number varchar(255) not null,
    text TEXT not null,
    title varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table leases (
    end_date date not null,
    monthly_rent decimal(10,2) not null,
    start_date date not null,
    id bigint not null auto_increment,
    tenant_id bigint not null,
    unit_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table maintenance_requests (
    scheduled_for date,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    resolved_at datetime(6),
    tenant_confirmed_at datetime(6),
    tenant_id bigint not null,
    unit_id bigint not null,
    ai_status varchar(30),
    category varchar(30),
    status varchar(30) not null,
    urgency varchar(30),
    ai_error TEXT,
    ai_summary varchar(255),
    damage_phrase varchar(255),
    damage_segmentation MEDIUMTEXT,
    description TEXT,
    photo_observations TEXT,
    photo_path varchar(255),
    transcript TEXT,
    urgency_reason TEXT,
    voice_note_path varchar(255),
    primary key (id)
) engine=InnoDB;

create table password_reset_tokens (
    created_at datetime(6) not null,
    expires_at datetime(6) not null,
    id bigint not null auto_increment,
    user_id bigint not null,
    token_hash varchar(64) not null,
    primary key (id)
) engine=InnoDB;

create table properties (
    id bigint not null auto_increment,
    manager_id bigint not null,
    address varchar(255) not null,
    name varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table tenancies (
    decided_at datetime(6),
    id bigint not null auto_increment,
    requested_at datetime(6) not null,
    tenant_id bigint not null,
    unit_id bigint not null,
    status varchar(30) not null,
    primary key (id)
) engine=InnoDB;

create table tenant_invites (
    lease_end date not null,
    lease_start date not null,
    monthly_rent decimal(10,2) not null,
    accepted_at datetime(6),
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    tenant_id bigint,
    unit_id bigint not null,
    email varchar(255) not null,
    full_name varchar(255) not null,
    phone varchar(255),
    primary key (id)
) engine=InnoDB;

create table units (
    id bigint not null auto_increment,
    property_id bigint not null,
    unit_number varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table vendors (
    id bigint not null auto_increment,
    category varchar(30) not null,
    email varchar(255) not null,
    name varchar(255) not null,
    phone varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table work_order_updates (
    scheduled_for date,
    author_id bigint not null,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    request_id bigint not null,
    status varchar(30) not null,
    note TEXT,
    primary key (id)
) engine=InnoDB;

alter table agent_drafts
   add constraint UKq0wtayn50jfxy1gipg2rbuc6d unique (request_id);

alter table app_users
   add constraint UK4vj92ux8a2eehds1mdvmks473 unique (email);

alter table password_reset_tokens
   add constraint UKajre85ybxavf1tt4omkrs5p6g unique (token_hash);

alter table agent_drafts
   add constraint FKcb9ufjroatirxfwp16wwkgttd
   foreign key (cited_clause_id)
   references lease_clauses (id);

alter table agent_drafts
   add constraint FK2yxbutx2iq4muqc5djqo1hiql
   foreign key (request_id)
   references maintenance_requests (id);

alter table agent_drafts
   add constraint FKd05kmngsuir7a0l5i7tkv2crw
   foreign key (vendor_id)
   references vendors (id);

alter table lease_clauses
   add constraint FK3oe55rjth676kjb5cet5kn965
   foreign key (lease_id)
   references leases (id);

alter table leases
   add constraint FKttuqkdka79ck0ub9ht62b57b
   foreign key (tenant_id)
   references app_users (id);

alter table leases
   add constraint FK5noeykhrutapru4f8jnx75sm4
   foreign key (unit_id)
   references units (id);

alter table maintenance_requests
   add constraint FK6oq76dl70san7yalji9urd3bu
   foreign key (tenant_id)
   references app_users (id);

alter table maintenance_requests
   add constraint FKg0yit7sqjjsyy3vftm7tkkhq3
   foreign key (unit_id)
   references units (id);

alter table password_reset_tokens
   add constraint FKk4vaguhrxs5jl3lqtshglaxo8
   foreign key (user_id)
   references app_users (id);

alter table properties
   add constraint FK3m8ejn2iicrrv5wuc9qvn29xs
   foreign key (manager_id)
   references app_users (id);

alter table tenancies
   add constraint FKhe8lmi6agx6lk2jq3ah1mj692
   foreign key (tenant_id)
   references app_users (id);

alter table tenancies
   add constraint FKl61nb25c3gu401i4vrbrw1i1l
   foreign key (unit_id)
   references units (id);

alter table tenant_invites
   add constraint FKjl4owu6jrp0qj1j62u37b0etq
   foreign key (tenant_id)
   references app_users (id);

alter table tenant_invites
   add constraint FKdo44pgtf19tlq5vj0y393e7u6
   foreign key (unit_id)
   references units (id);

alter table units
   add constraint FKsvctmn06wdb9r366vjpvf88j3
   foreign key (property_id)
   references properties (id);

alter table work_order_updates
   add constraint FKdpt2l947rmkiuxnaxflkrgquw
   foreign key (author_id)
   references app_users (id);

alter table work_order_updates
   add constraint FKejx7manqwra2ljonyuswgo00i
   foreign key (request_id)
   references maintenance_requests (id);
