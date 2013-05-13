%codeblock = type void ()
;
; Stack element types:
;
; 0 INTEGER
; 1 STRING
; 2 LITERAL
; 3 BOOL
; 4 CHAR
; 5 COLOUR
; 6 FLOAT
; 7 NULL
; 8 ARRAY
;
%stackelement = type {i8, i8*}
%stacktype = type [4096 x %stackelement]

@stack = internal global %stacktype zeroinitializer
@sp = internal global i16 zeroinitializer
@svalue = internal global %stackelement zeroinitializer

@printfstr = internal constant [7 x i8]  c" > %d\0A\00"
@printfstr_s = internal constant [7 x i8]  c" > %s\0A\00"
@printfstr_f = internal constant [7 x i8]  c" > %f\0A\00"
%printfstr_type = type [7 x i8]
@printflogstr = internal constant [19 x i8]  c"[log|Type:%d]> %d\0A\00"
@printflogstr_s = internal constant [19 x i8]  c"[log|Type:%d]> %s\0A\00"
@printflogstr_f = internal constant [19 x i8]  c"[log|Type:%d]> %f\0A\00"
%printflogstr_type = type [19 x i8]

@inlineprint = internal constant [3 x i8]  c"%s\00"
@inlineprintend = internal constant [4 x i8]  c"%s\0A\00"

@inttostr = internal constant [3 x i8]  c"%d\00"

@printnull = internal constant [11 x i8]  c" > !NULL!\0A\00"
@lognull = internal constant [23 x i8]  c"[log|Type:%d]> !NULL!\0A\00"

;User input prepend
@inputmsg = internal constant [7 x i8]  c" %s > \00"

@printexceptionstr = internal constant [12 x i8]  c"/!\\ %s /!\\\0A\00"
;Exception messages
@concatexception = internal constant [27 x i8]  c"Can only concat two strings"
@lengthexception = internal constant [41 x i8]  c"String_Length can only be used on strings"
@equalexception = internal constant [40 x i8]  c"String_Equal can only be used on strings"
@ifexception = internal constant [21 x i8]  c"Invalid IF comparison"
@whileexception = internal constant [24 x i8]  c"Invalid While comparison"
@calculationexception = internal constant [43 x i8]  c"Can only perform calculations on same types"
@outofboundsexception = internal constant [22 x i8]  c"Index is out of bounds"


declare i32 @printf(i8*, ...)
declare i32 @sprintf(i8*, i8*, ...)
declare i32 @puts(i8*)
declare i8* @strcat(i8*, i8*)
declare i8* @strlen(i8*)
declare i32 @strcmp(i8*, i8*)
declare i32 @strncpy(i8*, i8*, i32)
declare i32 @strcpy(i8*, i8*)
declare i32* @gets(i32*)
declare i32 @strtol(i8*, i8**, i32)
declare void @exit(i32)

define void @push(i8 %kind, i8* %value) {
	%sp = load i16* @sp
	%loc = getelementptr %stacktype* @stack, i32 0, i16 %sp
	%vse = insertvalue %stackelement undef, i8 %kind, 0
	%vse1 = insertvalue %stackelement %vse, i8* %value, 1
	store %stackelement %vse1, %stackelement* %loc
	%newsp = add i16 1, %sp
	store i16 %newsp, i16* @sp
	ret void
}

define void @pop() {
	%sp = load i16* @sp
	%newsp = sub i16 %sp, 1
	%vloc = getelementptr %stacktype* @stack, i32 0, i16 %newsp
	%vse = load %stackelement* %vloc
	store %stackelement %vse, %stackelement* @svalue
	store i16 %newsp, i16* @sp
	ret void
}

define void @print_top() {
	call void @pop()
	%vloc = getelementptr %stackelement* @svalue, i32 0, i32 1
	%valp = load i8** %vloc
	%val = ptrtoint i8* %valp to i32
	%tmp15 = call i32 (i8*, ...)* @printf(i8* getelementptr ([7 x i8]* @printfstr, i32 0, i32 0), i32 %val )

	ret void
}

define i32 @get_top() {
	call void @pop()
	%vloc = getelementptr %stackelement* @svalue, i32 0, i32 1
	%valp = load i8** %vloc
	%rv = ptrtoint i8* %valp to i32

	ret i32 %rv
}

define void @throw_exception(i8* %exceptionmessage){
	call i32 (i8*, ...)* @printf(i8* getelementptr ([12 x i8]* @printexceptionstr, i32 0, i32 0), i8* %exceptionmessage )
	call void @exit(i32 1)
	unreachable
}

define i32 @fun_String_Concat(%stackelement %dest, %stackelement %src) {
	entry:
		%dest_type = extractvalue  %stackelement %dest, 0
		%src_type = extractvalue  %stackelement %src, 0
		%firstif = icmp eq i8 1, %dest_type
		br i1 %firstif, label %typeMatch, label %notok
	typeMatch:
		%secondif = icmp eq i8 1, %src_type
		br i1 %secondif, label %ok, label %notok
	ok:
		%dest_val = extractvalue  %stackelement %dest, 1
		%src_val = extractvalue  %stackelement %src, 1
		;allocate new block of memory for total size
		%len1ptr = call i8* @strlen(i8* %dest_val)
		%len1 = ptrtoint i8* %len1ptr to i32
		%len2ptr = call i8* @strlen(i8* %src_val)
		%len2 = ptrtoint i8* %len2ptr to i32
		%totallen = add i32 %len1, %len2

		%finalstring = alloca i8, i32 %totallen
		%concat1 = call i32 @strcpy(i8* %finalstring, i8* %dest_val)
		%resultp = call i8* @strcat(i8* %finalstring, i8* %src_val)
		%resultval = ptrtoint i8* %resultp to i32
		call void @push(i8 1, i8* %resultp)
		ret i32 %resultval
	notok:
		%exception = getelementptr [27 x i8]* @concatexception, i32 0, i32 0
		call void @throw_exception(i8* %exception)
		ret i32 0
}

define i32 @fun_String_Length(%stackelement %str){
	entry:
		%str_type = extractvalue  %stackelement %str, 0
		%str_val = extractvalue  %stackelement %str, 1
		%firstif = icmp eq i8 1, %str_type
		br i1 %firstif, label %ok, label %notok
	ok:
		%resultp = call i8* @strlen(i8* %str_val)
		%resultval = ptrtoint i8* %resultp to i32
		call void @push(i8 0, i8* %resultp)
		ret i32 %resultval
	notok:
		%exception = getelementptr [41 x i8]* @lengthexception, i32 0, i32 0
		call void @throw_exception(i8* %exception)
		ret i32 0
}

define i32 @fun_String_Equal(%stackelement %str1,%stackelement %str2){
	entry:
		%str1_type = extractvalue  %stackelement %str1, 0
		%str1_val = extractvalue  %stackelement %str1, 1
		%str2_type = extractvalue  %stackelement %str2, 0
		%str2_val = extractvalue  %stackelement %str2, 1
		%firstif = icmp eq i8 1, %str1_type
		br i1 %firstif, label %typeMatch, label %notok
	typeMatch:
		%secondif = icmp eq i8 1, %str2_type
		br i1 %secondif, label %ok, label %notok
	ok:
		%resultval = call i32 @strcmp(i8* %str1_val, i8* %str2_val)
		%equalif = icmp eq i32 0, %resultval
		br i1 %equalif, label %isequal, label %notequal
	isequal:
		%true = inttoptr i1 1 to i8*
		call void @push(i8 3, i8* %true)
		ret i32 %resultval
	notequal:
		%false = inttoptr i1 0 to i8*
		call void @push(i8 3, i8* %false)
		ret i32 %resultval
	notok:
		%exception = getelementptr [40 x i8]* @equalexception, i32 0, i32 0
		call void @throw_exception(i8* %exception)
		ret i32 0
}

define i32 @fun_Get_Input(%stackelement %str){
	entry:
		%str_val = extractvalue %stackelement %str, 1
		%strptr = getelementptr i8* %str_val
		%printmsg = call i32 (i8*, ...)* @printf(i8* getelementptr ([7 x i8]* @inputmsg, i32 0, i32 0), i8* %strptr )		
		%inputbuffer = alloca i32
		%inputbufferptr = getelementptr i32* %inputbuffer
		%getcall = call i32* @gets(i32* %inputbufferptr)
		%text = ptrtoint i32* %inputbufferptr to i32
		%val = bitcast i32* %inputbufferptr to i8*
		call void @push(i8 1, i8* %val)
		ret i32 %text
}

define i32 @fun_String_To_Integer(%stackelement %str){
	entry:
		%str_val = extractvalue %stackelement %str, 1
		%strptr = getelementptr i8* %str_val
    	%returnval = call i32 @strtol (i8* %strptr, i8** null, i32 10)
		%returnptr2 = inttoptr i32 %returnval to i8*
		call void @push(i8 0, i8* %returnptr2)
		ret i32 %returnval
}

define i32 @fun_Integer_To_String(%stackelement %str){
	entry:
		%str_val = extractvalue %stackelement %str, 1
		%strptr = getelementptr i8* %str_val
		%strval = ptrtoint i8* %strptr to i32
		%output = alloca i8
    	%returnval = call i32 (i8*, i8*, ...)* @sprintf(i8* %output, i8* getelementptr ([3 x i8]* @inttostr, i32 0, i32 0), i32 %strval)
		%outputint = ptrtoint i8* %output to i32
		call void @push(i8 1, i8* %output)
		ret i32 %outputint
}

define i32 @fun_Float_To_Integer(%stackelement %flt){
	entry:
		%fltvalptr = extractvalue %stackelement %flt, 1
		%fltvalptr2 = bitcast i8* %fltvalptr to double*
		%fltval = load double* %fltvalptr2
		%intval = fptosi double %fltval to i32
		%intptr = inttoptr i32 %intval to i8*
		call void @push(i8 0, i8* %intptr)
		ret i32 %intval
}

define i32 @fun_Integer_To_Float(%stackelement %int){
	entry:
		%intvalptr = extractvalue %stackelement %int, 1
		%intval = ptrtoint i8* %intvalptr to i32
		%fltval = sitofp i32 %intval to double
		%flttmpalloc = alloca double
		store double %fltval, double* %flttmpalloc
		%fltptr = bitcast double* %flttmpalloc to i8*
		call void @push(i8 6, i8* %fltptr)
		ret i32 %intval
}

define i32 @fun_IsInt(%stackelement %obj){
	entry:
		%intval = extractvalue %stackelement %obj, 0
		%if = icmp eq i8 0, %intval
		br i1 %if, label %returntrue, label %returnfalse
	returntrue:
		%true = inttoptr i1 1 to i8*
		call void @push(i8 3, i8* %true)
		ret i32 1
	returnfalse:
		%false = inttoptr i1 0 to i8*
		call void @push(i8 3, i8* %false)
		ret i32 0
}
define i32 @fun_IsString(%stackelement %obj){
	entry:
		%intval = extractvalue %stackelement %obj, 0
		%if = icmp eq i8 1, %intval
		br i1 %if, label %returntrue, label %returnfalse
	returntrue:
		%true = inttoptr i1 1 to i8*
		call void @push(i8 3, i8* %true)
		ret i32 1
	returnfalse:
		%false = inttoptr i1 0 to i8*
		call void @push(i8 3, i8* %false)
		ret i32 0
}
define i32 @fun_IsBoolean(%stackelement %obj){
	entry:
		%intval = extractvalue %stackelement %obj, 0
		%if = icmp eq i8 3, %intval
		br i1 %if, label %returntrue, label %returnfalse
	returntrue:
		%true = inttoptr i1 1 to i8*
		call void @push(i8 3, i8* %true)
		ret i32 1
	returnfalse:
		%false = inttoptr i1 0 to i8*
		call void @push(i8 3, i8* %false)
		ret i32 0
}
define i32 @fun_IsChar(%stackelement %obj){
	entry:
		%intval = extractvalue %stackelement %obj, 0
		%if = icmp eq i8 4, %intval
		br i1 %if, label %returntrue, label %returnfalse
	returntrue:
		%true = inttoptr i1 1 to i8*
		call void @push(i8 3, i8* %true)
		ret i32 1
	returnfalse:
		%false = inttoptr i1 0 to i8*
		call void @push(i8 3, i8* %false)
		ret i32 0
}
define i32 @fun_IsColour(%stackelement %obj){
	entry:
		%intval = extractvalue %stackelement %obj, 0
			%if = icmp eq i8 5, %intval
			br i1 %if, label %returntrue, label %returnfalse
	returntrue:
		%true = inttoptr i1 1 to i8*
		call void @push(i8 3, i8* %true)
		ret i32 1
	returnfalse:
		%false = inttoptr i1 0 to i8*
		call void @push(i8 3, i8* %false)
		ret i32 0
}
define i32 @fun_IsFloat(%stackelement %obj){
	entry:
		%intval = extractvalue %stackelement %obj, 0
		%if = icmp eq i8 6, %intval
		br i1 %if, label %returntrue, label %returnfalse
	returntrue:
		%true = inttoptr i1 1 to i8*
		call void @push(i8 3, i8* %true)
		ret i32 1
	returnfalse:
		%false = inttoptr i1 0 to i8*
		call void @push(i8 3, i8* %false)
		ret i32 0
}
define i32 @fun_IsNull(%stackelement %obj){
	entry:
		%intval = extractvalue %stackelement %obj, 0
		%if = icmp eq i8 7, %intval
		br i1 %if, label %returntrue, label %returnfalse
	returntrue:
		%true = inttoptr i1 1 to i8*
		call void @push(i8 3, i8* %true)
		ret i32 1
	returnfalse:
		%false = inttoptr i1 0 to i8*
		call void @push(i8 3, i8* %false)
		ret i32 0
}

define i32 @fun_Print_Inline(%stackelement %str, %stackelement %endline) {
	%str_val = extractvalue %stackelement %str, 1
	%endlineptr = extractvalue %stackelement %endline, 1
	%strptr = getelementptr i8* %str_val
	%endlineval = ptrtoint i8* %endlineptr to i32
	%if = icmp eq i32 1, %endlineval
	br i1 %if, label %doend, label %dontend
	dontend:
		%printy1 = call i32 (i8*, ...)* @printf(i8* getelementptr ([3 x i8]* @inlineprint, i32 0, i32 0), i8* %strptr)
		ret i32 0
	doend:
		%printy2 = call i32 (i8*, ...)* @printf(i8* getelementptr ([4 x i8]* @inlineprintend, i32 0, i32 0), i8* %strptr)
		ret i32 0
}


define void @main() {
	call i32 @fun_Start()
	ret void
}

