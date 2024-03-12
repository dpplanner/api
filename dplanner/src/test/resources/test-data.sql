insert into member (id) values (1);
insert into club (id) values (1);
insert into resource (id,club_id,RETURN_MESSAGE_REQUIRED) values (1,1,false);
insert into club_member (id,member_id,club_id,is_confirmed) values (1,1,1,true);