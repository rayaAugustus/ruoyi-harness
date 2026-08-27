-- RuoYi Harness schema and administration menus (MySQL 8+)
create table if not exists harness_app (
  id bigint not null auto_increment,
  app_key varchar(64) not null,
  name varchar(128) not null,
  description text,
  route_title varchar(128) not null,
  icon varchar(64),
  order_num int not null default 0,
  required_permission varchar(128) not null,
  enabled tinyint(1) not null default 1,
  published_version_id bigint null,
  created_by bigint not null,
  created_at datetime(3) not null,
  updated_by bigint not null,
  updated_at datetime(3) not null,
  primary key (id),
  unique key uk_harness_app_key (app_key)
) engine=InnoDB default charset=utf8mb4;

create table if not exists harness_app_version (
  id bigint not null auto_increment,
  app_id bigint not null,
  version_no bigint not null,
  sdk_version varchar(16) not null,
  source longtext not null,
  source_hash varchar(80),
  status varchar(24) not null,
  created_by bigint not null,
  created_at datetime(3) not null,
  validated_at datetime(3),
  published_at datetime(3),
  primary key (id),
  unique key uk_harness_app_version (app_id,version_no),
  constraint fk_harness_version_app foreign key (app_id) references harness_app(id)
) engine=InnoDB default charset=utf8mb4;

create table if not exists harness_execution_log (
  id bigint not null auto_increment,
  trace_id varchar(64) not null,
  request_id varchar(128),
  app_id bigint not null,
  app_version_id bigint not null,
  user_id bigint not null,
  entry_type varchar(16) not null,
  entry_name varchar(128) not null,
  status varchar(24) not null,
  started_at datetime(3) not null,
  finished_at datetime(3) not null,
  elapsed_ms bigint not null,
  capability_calls int not null,
  error_code varchar(64),
  error_summary varchar(512),
  primary key (id),
  key idx_harness_execution_trace (trace_id),
  key idx_harness_execution_app_time (app_id,started_at)
) engine=InnoDB default charset=utf8mb4;

create table if not exists harness_capability_log (
  id bigint not null auto_increment,
  trace_id varchar(64) not null,
  execution_log_id bigint,
  capability_name varchar(128) not null,
  capability_version varchar(32) not null,
  user_id bigint not null,
  risk_level varchar(32) not null,
  status varchar(24) not null,
  elapsed_ms bigint not null,
  error_code varchar(64),
  created_at datetime(3) not null,
  primary key (id),
  key idx_harness_capability_trace (trace_id),
  key idx_harness_capability_name_time (capability_name,created_at)
) engine=InnoDB default charset=utf8mb4;

create table if not exists harness_ai_session (
  id bigint not null auto_increment,
  session_key varchar(64) not null,
  app_id bigint null,
  active_version_id bigint null,
  created_by bigint not null,
  created_at datetime(3) not null,
  updated_at datetime(3) not null,
  title varchar(255) not null,
  status varchar(24) not null,
  primary key (id),
  unique key uk_harness_ai_session_key (session_key),
  key idx_harness_ai_session_user_time (created_by,updated_at),
  constraint fk_harness_ai_session_app foreign key (app_id) references harness_app(id),
  constraint fk_harness_ai_session_version foreign key (active_version_id) references harness_app_version(id)
) engine=InnoDB default charset=utf8mb4;

create table if not exists harness_ai_message (
  id bigint not null auto_increment,
  session_id bigint not null,
  role varchar(24) not null,
  content text not null,
  script_snapshot longtext null,
  model varchar(128) null,
  provider varchar(64) null,
  input_tokens bigint null,
  output_tokens bigint null,
  created_at datetime(3) not null,
  primary key (id),
  key idx_harness_ai_message_session (session_id,id),
  constraint fk_harness_ai_message_session foreign key (session_id) references harness_ai_session(id)
) engine=InnoDB default charset=utf8mb4;

-- Stable Harness root and administration routes. Dynamic apps are synchronized below menu 2100.
insert into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select 2100,'Harness',0,8,'harness',null,null,'Harness','1','1','M','0','0','', 'component','admin',sysdate(),'Dynamic script applications'
where not exists(select 1 from sys_menu where menu_id=2100);
insert into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select 2101,'Applications',2100,1,'applications','harness/admin/applications','HarnessApplications','1','1','C','0','0','harness:app:list','list','admin',sysdate()
where not exists(select 1 from sys_menu where menu_id=2101);
insert into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select 2102,'Execution Logs',2100,2,'execution-logs','harness/admin/executions','HarnessExecutionLogs','1','1','C','0','0','harness:audit:view','log','admin',sysdate()
where not exists(select 1 from sys_menu where menu_id=2102);
insert into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select 2103,'Capabilities',2100,3,'capabilities','harness/admin/capabilities','HarnessCapabilities','1','1','C','0','0','harness:app:list','skill','admin',sysdate()
where not exists(select 1 from sys_menu where menu_id=2103);
insert into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select 2104,'AI Builder',2100,0,'ai-builder','harness/ai/builder','HarnessAiBuilder','1','1','C','0','0','harness:ai:use','magic-stick','admin',sysdate()
where not exists(select 1 from sys_menu where menu_id=2104);

insert into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select 2110,'Create app',2101,1,'',null,'1','1','F','0','0','harness:app:create','#','admin',sysdate() where not exists(select 1 from sys_menu where menu_id=2110);
insert into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select 2111,'Edit app',2101,2,'',null,'1','1','F','0','0','harness:app:edit','#','admin',sysdate() where not exists(select 1 from sys_menu where menu_id=2111);
insert into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select 2112,'Validate app',2101,3,'',null,'1','1','F','0','0','harness:app:validate','#','admin',sysdate() where not exists(select 1 from sys_menu where menu_id=2112);
insert into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select 2113,'Publish app',2101,4,'',null,'1','1','F','0','0','harness:app:publish','#','admin',sysdate() where not exists(select 1 from sys_menu where menu_id=2113);
insert into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select 2114,'Rollback app',2101,5,'',null,'1','1','F','0','0','harness:app:rollback','#','admin',sysdate() where not exists(select 1 from sys_menu where menu_id=2114);
insert into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select 2115,'Enable/disable app',2101,6,'',null,'1','1','F','0','0','harness:app:disable','#','admin',sysdate() where not exists(select 1 from sys_menu where menu_id=2115);
insert into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select 2116,'Use AI Builder',2104,1,'',null,'1','1','F','0','0','harness:ai:use','#','admin',sysdate() where not exists(select 1 from sys_menu where menu_id=2116);
insert into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select 2117,'List AI sessions',2104,2,'',null,'1','1','F','0','0','harness:ai:session:list','#','admin',sysdate() where not exists(select 1 from sys_menu where menu_id=2117);
insert into sys_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select 2118,'View AI session',2104,3,'',null,'1','1','F','0','0','harness:ai:session:view','#','admin',sysdate() where not exists(select 1 from sys_menu where menu_id=2118);

-- Grant Harness administration permissions to the built-in administrator role.
insert into sys_role_menu(role_id,menu_id) select 1,m.menu_id from sys_menu m where m.menu_id between 2100 and 2118 and not exists(select 1 from sys_role_menu rm where rm.role_id=1 and rm.menu_id=m.menu_id);
