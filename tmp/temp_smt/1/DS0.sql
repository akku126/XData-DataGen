delete from prereq
delete from takes
delete from teaches
delete from section
delete from course
delete from advisor
delete from instructor
delete from student
delete from department
delete from classroom
insert into classroom values ('Taylor','3128','462')
insert into classroom values ('Packard','120','10')
insert into department values ('History','Packard','62572.0')
insert into department values ('Finance','Taylor','50000.0')
insert into department values ('Finance','Taylor','50000.0')
insert into department values ('Finance','Taylor','50000.0')
insert into department values ('Finance','Taylor','50000.0')
insert into department values ('History','Packard','62572.0')
insert into department values ('Finance','Taylor','50000.0')
insert into instructor values ('10101','Einstein','Finance','40000.0')
insert into instructor values ('10101','Einstein','Finance','40000.0')
insert into instructor values ('12121','Katz',null,'53403.5')
insert into course values ('BIO-301','Music Video Production','History','3')
insert into course values ('CS-347','Robotics','Finance','3')
insert into course values ('BIO-301','Music Video Production','History','3')
insert into course values ('CS-347','Robotics','Finance','3')
insert into section values ('BIO-301','1','Summer','2009','Taylor','3128','B')
insert into section values ('CS-347','2','Summer','2009','Packard','120','C')
insert into teaches values ('12121','BIO-301','1','Summer','2009')
insert into teaches values ('10101','CS-347','2','Summer','2009')
