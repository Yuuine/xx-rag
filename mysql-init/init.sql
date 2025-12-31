create table rag.rag_documents
(
    id         bigint auto_increment
        primary key,
    file_md5   char(32)                           not null,
    file_name  varchar(255)                       not null,
    created_at datetime default CURRENT_TIMESTAMP null,
    constraint file_md5
        unique (file_md5)
);

create index idx_created
    on rag.rag_documents (created_at);

