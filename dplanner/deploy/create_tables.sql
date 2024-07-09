-- Create Tables
create table attachment (
    id bigint not null,
    type varchar(255) not null check (type in ('VIDEO','IMAGE','NONE')),
    url varchar(255),
    post_id bigint,
    reservation_id bigint,
    created_date timestamp(6),
    last_modified_date timestamp(6),
    primary key (id)
);

create table club (
    id bigint not null,
    club_name varchar(255),
    info varchar(255),
    url varchar(255),
    primary key (id)
);

create table club_authority (
    id bigserial not null,
    club_id bigint,
    name varchar(255),
    description varchar(255),
    primary key (id)
);

create table club_authority_club_authority_types (
    club_authority_id bigint not null,
    club_authority_types varchar(255) not null check (club_authority_types in ('MEMBER_ALL','SCHEDULE_ALL','POST_ALL','RESOURCE_ALL','NONE'))
);

create table club_member (
    id bigserial not null,
    club_id bigint,
    club_authority_id bigint,
    member_id bigint,
    name varchar(255),
    role varchar(255) not null check (role in ('ADMIN','MANAGER','USER','NONE')),
    info varchar(255),
    is_confirmed boolean,
    is_deleted boolean,
    url varchar(255),
    primary key (id)
);

create table comment (
    id bigint not null,
    club_id bigint,
    club_member_id bigint,
    post_id bigint,
    parent_id bigint,
    content varchar(255),
    is_deleted boolean,
    created_date timestamp(6),
    last_modified_date timestamp(6),
    primary key (id)
);

create table comment_member_like (
    id bigint not null,
    club_member_id bigint,
    comment_id bigint,
    primary key (id)
);

create table comment_report (
    id bigint not null,
    club_member_id bigint,
    comment_id bigint,
    message varchar(255),
    primary key (id)
);

create table invite_code (
    id bigint not null,
    club_id bigint,
    code varchar(255),
    created_date timestamp(6),
    last_modified_date timestamp(6),
    primary key (id)
);

create table lock (
    id bigserial not null,
    resource_id bigint,
    message varchar(255),
    start_date_time timestamp(6),
    end_date_time timestamp(6),
    created_date timestamp(6),
    last_modified_date timestamp(6),
    primary key (id)
);

create table member (
    id bigint not null,
    recent_club_id bigint,
    email varchar(255),
    name varchar(255),
    is_deleted boolean,
    fcm_token varchar(255),
    refresh_token varchar(255),
    eula boolean default false,
    created_date timestamp(6),
    last_modified_date timestamp(6),
    primary key (id)
);

create table post (
    id bigint not null,
    club_id bigint,
    club_member_id bigint,
    title varchar(255),
    content text,
    is_fixed boolean,
    created_date timestamp(6),
    last_modified_date timestamp(6),
    primary key (id)
);

create table post_member_like (
    id bigint not null,
    club_member_id bigint,
    post_id bigint,
    primary key (id)
);

create table post_report (
    id bigint not null,
    club_member_id bigint,
    post_id bigint,
    message varchar(255),
    primary key (id)
);

create table private_message (
    id bigint not null,
    club_member_id bigint,
    title varchar(255),
    content varchar(255),
    type varchar(255) not null check (type in ('ACCEPT','INFO','NOTICE','REJECT','REPORT','REQUEST')),
    info_type varchar(255) not null check (info_type in ('POST','RESERVATION','MEMBER','RETURN')),
    info varchar(255),
    is_read boolean,
    redirect_url varchar(255),
    created_date timestamp(6),
    last_modified_date timestamp(6),
    primary key (id)
);

create table reservation (
    id bigserial not null,
    club_member_id bigint,
    resource_id bigint,
    start_date_time timestamp(6),
    end_date_time timestamp(6),
    title varchar(255),
    usage varchar(255),
    status varchar(255) check (status in ('REQUEST','CONFIRMED','REJECTED')),
    sharing boolean not null,
    is_returned boolean not null,
    reject_message varchar(255),
    return_message varchar(255),
    created_date timestamp(6),
    last_modified_date timestamp(6),
    primary key (id)
);

create table reservation_invitee (
    id bigint not null,
    club_member_id bigint,
    reservation_id bigint,
    primary key (id)
);

create table resource (
    id bigserial not null,
    club_id bigint,
    name varchar(255),
    resource_type varchar(255) not null check (resource_type in ('PLACE','THING')),
    bookable_span bigint,
    return_message_required boolean not null,
    info varchar(255),
    notice text,
    created_date timestamp(6),
    last_modified_date timestamp(6),
    primary key (id)
);

create table post_block (
    club_member_id bigint not null,
    post_id bigint not null,
    primary key (club_member_id, post_id)
);

create table club_member_block (
    club_member_id bigint not null,
    blocked_club_member_id bigint not null,
    primary key (blocked_club_member_id, club_member_id)
);

-- Add Constraints
alter table if exists attachment
    drop constraint if exists UK_ofevfnbd2scd80rbeciujvcts;

alter table if exists attachment
    add constraint UK_ofevfnbd2scd80rbeciujvcts unique (url);

-- Create Sequences
create sequence attachment_seq start with 1 increment by 50;
create sequence club_seq start with 1 increment by 50;
create sequence comment_member_like_seq start with 1 increment by 50;
create sequence comment_report_seq start with 1 increment by 50;
create sequence comment_seq start with 1 increment by 50;
create sequence invite_code_seq start with 1 increment by 50;
create sequence member_seq start with 1 increment by 50;
create sequence post_member_like_seq start with 1 increment by 50;
create sequence post_report_seq start with 1 increment by 50;
create sequence post_seq start with 1 increment by 50;
create sequence private_message_seq start with 1 increment by 50;
create sequence reservation_invitee_seq start with 1 increment by 1;

-- Add Foreign Keys
alter table if exists club_authority_club_authority_types
    add constraint FKfygxgqlf3b3iame6upnsyqnwr
    foreign key (club_authority_id)
    references club_authority;
