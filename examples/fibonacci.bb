init N;

clear F;
clear G;
clear H;

incr G;

while N not 0 do;
  copy G to H;
  while F not 0 do;
    incr H;
    decr F;
  end;

  copy G to F;
  copy H to G;

  decr N;
end;
