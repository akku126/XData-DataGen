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
insert into classroom values ('Packard','3128','170')
insert into classroom values ('Taylor','120','10')
insert into department values ('Music','Painter','99021.0')
insert into department values ('Comp. Sci.','Painter','102614.5445')
insert into department values ('History','Taylor','65974.0')
insert into department values ('Finance','Taylor','102614.5445')
insert into department values ('Finance','Taylor','102614.5445')
insert into department values ('Finance','Taylor','102614.5445')
insert into department values ('Music','Painter','99021.0')
insert into department values ('Physics','Packard','109929.5445')
insert into instructor values ('10101','Einstein','Finance','40000.0')
insert into instructor values ('10101','Einstein','Finance','40000.0')
insert into instructor values ('12121','Katz','History',null)
insert into instructor values ('10101','Einstein','Finance','40000.0')
insert into course values ('CS-347','Music Video Production','Music','3')
insert into course values ('BIO-301','Robotics',null,'4')
insert into course values ('CS-347','Music Video Production','Music','3')
insert into course values ('BIO-301','Robotics',null,'4')
insert into section values ('CS-347','1','Summer','2010','Packard','3128','B')
insert into section values ('BIO-301','2','Spring','2009','Taylor',null,'C')
insert into teaches values ('12121','CS-347','1','Summer','2010')
insert into teaches values ('10101','BIO-301','2','Spring','2009')
