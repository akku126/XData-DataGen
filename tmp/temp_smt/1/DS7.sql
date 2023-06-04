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
insert into classroom values ('Taylor','120','170')
insert into classroom values ('Painter','3128','291')
insert into classroom values ('Taylor','120','170')
insert into department values ('Finance','Taylor','50000.0')
insert into department values ('Finance','Taylor','50000.0')
insert into department values ('Physics','Packard','111985.0')
insert into department values ('Finance','Taylor','50000.0')
insert into department values ('Music','Watson',null)
insert into department values ('Finance','Taylor','50000.0')
insert into department values ('History','Painter','67059.0')
insert into department values ('Comp. Sci.','Taylor','58652.0')
insert into department values ('Finance','Taylor','50000.0')
insert into department values ('Finance','Taylor','50000.0')
insert into instructor values ('10101','Einstein',null,'40000.0')
insert into instructor values ('10101','Einstein',null,'40000.0')
insert into instructor values ('10101','Einstein',null,'40000.0')
insert into instructor values ('10101','Einstein',null,'40000.0')
insert into course values ('BIO-301','Robotics','Finance','3')
insert into course values ('BIO-301','Robotics','Finance','3')
insert into course values ('BIO-301','Robotics','Finance','3')
insert into course values ('CS-347','Music Video Production',null,'4')
insert into course values ('BIO-301','Robotics','Finance','3')
insert into course values ('CS-347','Music Video Production',null,'4')
insert into section values ('BIO-301','2','Spring','2009','Taylor','120','C')
insert into section values ('CS-347','1','Summer','2010','Painter','3128','B')
insert into section values ('BIO-301','2','Spring','2009','Taylor','120','C')
insert into teaches values ('10101','BIO-301','2','Spring','2009')
insert into teaches values ('10101','CS-347','1','Summer','2010')
insert into teaches values ('10101','BIO-301','2','Spring','2009')
