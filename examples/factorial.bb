init N;

clear H;
clear G;
clear F;
incr F;
decr N;

while N not 0 do;
  copy F to G;
  while G not 0 do;
    copy N to H;
    while H not 0 do;
      incr F;
      decr H;
    end;
    decr G;
  end;
  decr N;
end;