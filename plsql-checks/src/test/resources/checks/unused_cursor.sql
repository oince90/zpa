declare
  cursor cur is -- Noncompliant {{Remove this unused "cur" cursor.}}
    select 1 from dual;
    
  cursor cur2 is
    select 1 from dual;
begin
  open cur2;
end;

create package pkg is
  cursor cur is -- compliant, this is a package specification, we don't know if there are any usages
    select 1 from dual;
end;