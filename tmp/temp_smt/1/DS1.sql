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
insert into classroom values ('Taylor','3128','10')
insert into department values ('History','Taylor',null)
insert into department values ('Physics','Taylor',null)
insert into department values ('Finance','Taylor',null)
insert into department values ('Music','Taylor',null)
insert into instructor values ('10101','Einstein','Finance','40209.0')
insert into instructor values ('12121','Einstein',null,'40000.0')
insert into course values ('BIO-301','Music Video Production',null,'3')
insert into course values ('BIO-301','Music Video Production',null,'3')
insert into section values ('BIO-301','2','Summer','2009','Taylor','3128','B')
insert into teaches values ('10101','BIO-301','2','Summer','2009')
